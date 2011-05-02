// photoshop_3block.h: interface for the Photoshop3Block class.
//
//////////////////////////////////////////////////////////////////////

#ifndef INCLUDE_PHOTOSHOP_3BLOCK
#define INCLUDE_PHOTOSHOP_3BLOCK

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <vector>


using std::string;

#include "iptc.h"
#include "byte_swapping.h"

using std::vector;

// const unsigned short Iptc::tag_bim_iptc_ = 0x0404; // The bim type for IPTC data
//
#define tag_8bim 0x4d494238 // "8BIM"
#define tag_bim_iptc_ 0x0404 // The bim type for IPTC data

namespace jpeg_redaction {
class Photoshop3Block
{
  public:
  class BIM
  {
  public:
//    static const unsigned int tag_bim_ = 0x4d494238; // "8BIM"
    BIM(FILE *pFile) : iptc_(NULL) {
      int iRV = fread(&magic_, sizeof(magic_), 1, pFile);

      if (magic_ != tag_8bim)
        printf("BIM was 0x%x not 0x %x\n", magic_, tag_8bim);
      iRV = fread(&bim_type_, sizeof(bim_type_), 1, pFile);

      iRV = fread(&pascalstringlength_, sizeof(pascalstringlength_), 1, pFile);

      // The pascal string storage (including the length byte) must be even.
      // How many more bytes do we have to read?
      unsigned char pascalstringlengthrounded = pascalstringlength_ + (1-(pascalstringlength_%2));
      pascalstring_.resize(pascalstringlengthrounded);
      iRV = fread(&pascalstring_[0], sizeof(unsigned char), pascalstringlengthrounded, pFile);

      iRV = fread(&bim_length_, sizeof(bim_length_), 1, pFile);

      bim_length_ = byteswap4(bim_length_);

      unsigned int bim_length_rounded = bim_length_ + (bim_length_%2);  // Rounded to be even.
      if (bim_type_ == tag_bim_iptc_) {
        iptc_ = new Iptc(pFile, bim_length_);
      } else {
        printf("Got a BIM of type %x", bim_type_);
        data_.resize(bim_length_rounded);
        iRV = fread(&data_[0], sizeof(unsigned char), bim_length_rounded, pFile);
      }
//      throw("Got an unsupported BIM");
    }

    virtual ~BIM() { }

    int Length() const { return bim_length_; }
    // BIM data (rounded) , Bimlength (4) , Bim type (2) , pascal string (length + rounded)
    int TotalLength() const { return sizeof(bim_type_) + sizeof(magic_) + bim_length_  + (bim_length_%2) + sizeof(bim_length_) +
        pascalstringlength_ +  sizeof(pascalstringlength_) + 1  - (pascalstringlength_%2);}

    int Write(FILE *pFile) {
      int length = 0;
      int iRV;
      iRV = fwrite(&magic_, sizeof(magic_), 1, pFile);
      unsigned int bim_tag = tag_8bim;
      iRV = fwrite(&bim_tag, sizeof(unsigned int), 1, pFile);

      iRV = fwrite(&bim_type_, sizeof(unsigned short), 1, pFile);

      iRV = fwrite(&pascalstringlength_, sizeof(unsigned char), 1, pFile);

      // Number of bytes to write out - to make the (length + string) structure even length.
      unsigned char pascalstringlengthrounded = pascalstringlength_ + (1-(pascalstringlength_%2));
      iRV = fwrite(&pascalstring_[0], sizeof(unsigned char), pascalstringlengthrounded, pFile);

      unsigned int bim_length_rounded = bim_length_ + (bim_length_%2); // Rounded to be even.
      bim_length_rounded = byteswap4(bim_length_rounded);

      iRV = fwrite(&bim_length_rounded, sizeof(unsigned int), 1, pFile);
      iRV = fwrite(&data_[0], sizeof(unsigned char), bim_length_rounded, pFile);

      return length;
    }

    unsigned short type() const { return bim_type_;}
    unsigned char pascalstringlength_;
    unsigned short bim_type_;
    vector<unsigned char> pascalstring_;
    unsigned int bim_length_;
    vector<unsigned char> data_;
    Iptc *iptc_;
    unsigned short magic_;
  };


  // Read photoshop block from a file (ie a JPEG APP 13 block)
    Photoshop3Block(FILE *pFile, unsigned int recordlength) {
      char c;
      unsigned int remaininglength = recordlength;
      while((c=fgetc(pFile))!='\0'){
        headerstring_ += c;
      }

      if (strcmp(headerstring_.c_str(), "Photoshop 3.0")!=0)
        throw("Bad header in photoshop 3 block");
      remaininglength -= (headerstring_.length() + 1);
      while (remaininglength > 4) {
        unsigned int magic;
        BIM *bim = new BIM(pFile);
        int bimlength = bim->TotalLength();
        bims_.push_back(bim);
        remaininglength -= (bimlength);
      }
      if (remaininglength != 0) throw("Photoshop block length mismatch");
    }

    virtual ~Photoshop3Block() {
      for(int i = 0; i < bims_.size(); ++i) {
        delete bims_[i];
      }
      bims_.clear();
    }

    int Write(FILE *pFile) {
      int length = 0;
      int headerlen = headerstring_.length();
      int iRV = fwrite(headerstring_.c_str(), sizeof(unsigned char), headerlen + 1, pFile);
      if (iRV != headerlen + 1)
        throw("Can't write");
      length += iRV;

      for (int i = 0; i < bims_.size(); ++i) {
        iRV = bims_[i]->Write(pFile);
        length += iRV;
      }
      return length;
    }

    Iptc *GetIptc() {
      throw("This bit unimplemented\n");
      // for (int i = 0; i< bims_.size(); ++i)
      //   if (bims_[i]->GetIptc())
      //     return bims_[i]->GetIptc();
        return NULL;
    }

    std::string headerstring_;

    vector<BIM *> bims_;
};
}  // namespace jpeg_redaction
#endif // INCLUDE_PHOTOSHOP_3BLOCK
