// Copyright (C) 2011 Andrew W. Senior andrew.senior[AT]gmail.com
// Part of the Jpeg-Redaction-Library to read, parse, edit redact and
// write JPEG/EXIF/JFIF images.
// See https://github.com/asenior/Jpeg-Redaction-Library

// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.


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

#include "iptc.h"
#include "byte_swapping.h"

using std::string;
using std::vector;

// const unsigned short Iptc::tag_bim_iptc_ = 0x0404; // The bim type for IPTC data
//
#define tag_8bim 0x3842494d // 0x4d494238 // "8BIM"
#define tag_bim_iptc_ 0x0404 // The bim type for IPTC data

namespace jpeg_redaction {
class Photoshop3Block
{
  public:
  class BIM
  {
  public:
    BIM(FILE *pFile) : iptc_(NULL) {
      const bool arch_big_endian = ArchBigEndian();
      int iRV = fread(&magic_, sizeof(magic_), 1, pFile); // short

      if (!arch_big_endian)
	ByteSwapInPlace(&magic_, 1);
	
      if (magic_ != tag_8bim)
        printf("BIM was 0x%x not 0x%x\n", magic_, tag_8bim);

      // short
      iRV = fread(&bim_type_, sizeof(bim_type_), 1, pFile);

      iRV = fread(&pascalstringlength_, sizeof(pascalstringlength_), 1, pFile);

      // The pascal string storage (including the length byte) must be even.
      // How many more bytes do we have to read?
      unsigned char pascalstringlengthrounded =
	pascalstringlength_ + (1-(pascalstringlength_%2));
      pascalstring_.resize(pascalstringlengthrounded);
      iRV = fread(&pascalstring_[0], sizeof(unsigned char),
		  pascalstringlengthrounded, pFile);

      iRV = fread(&bim_length_, sizeof(bim_length_), 1, pFile);
      
      if (!arch_big_endian)
	ByteSwapInPlace(&bim_length_, 1);
      printf("bim_length read is %d\n", bim_length_);
      unsigned int bim_length_rounded =
	bim_length_ + (bim_length_%2);  // Rounded to be even.
      if (bim_type_ == tag_bim_iptc_) {
        iptc_ = new Iptc(pFile, bim_length_);
      } else {
        printf("Got a BIM of type %x size %d\n", bim_type_, bim_length_rounded);
        data_.resize(bim_length_rounded);
        iRV = fread(&data_[0], sizeof(unsigned char),
		    bim_length_rounded, pFile);
      }
//      throw("Got an unsupported BIM");
    }

    virtual ~BIM() { }

    int Length() const { return bim_length_; }
    // BIM data (rounded) , Bimlength (4) , Bim type (2) ,
    // pascal string (length + rounded)
    int TotalLength() const {
      return sizeof(bim_type_) + sizeof(magic_) + bim_length_  +
	(bim_length_%2) + sizeof(bim_length_) +
        pascalstringlength_ +  sizeof(pascalstringlength_) +
	1  - (pascalstringlength_%2);
    }

    int Write(FILE *pFile) {
      printf("Writing BIM3 at %zu\n", ftell(pFile));
      const bool arch_big_endian = ArchBigEndian();
      int length = 0;
      int iRV;
      unsigned int magic = tag_8bim;
      if (!arch_big_endian)
	ByteSwapInPlace(&magic, 1);
      iRV = fwrite(&magic, sizeof(magic), 1, pFile);
      length += sizeof(magic);

      unsigned short bim_type = bim_type_;
      if (!arch_big_endian)
	ByteSwapInPlace(&bim_type, 1);
      iRV = fwrite(&bim_type, sizeof(bim_type), 1, pFile);
      length += sizeof(bim_type);
      
      // Number of bytes to write out - to make the (length + string)
      // structure even length.
      
      iRV = fwrite(&pascalstringlength_, sizeof(pascalstringlength_), 1, pFile);
      length += sizeof(pascalstringlength_);

      unsigned char pascalstringlengthrounded =
	pascalstringlength_ + (1-(pascalstringlength_%2));
      iRV = fwrite(&pascalstring_[0], sizeof(unsigned char),
		   pascalstringlengthrounded, pFile);
      length += iRV;

      // Rounded to be even.
      const unsigned int bim_length_rounded = bim_length_ + (bim_length_%2);
      unsigned int bim_length_rounded_swap = bim_length_rounded;
      if (!arch_big_endian)
	ByteSwapInPlace(&bim_length_rounded_swap, 1);

      iRV = fwrite(&bim_length_rounded_swap, sizeof(unsigned int), 1, pFile);
      length += sizeof(unsigned int);

      printf("Writing BIM length %d, %p %zu\n",
	     bim_length_rounded, &data_[0], data_.size());
      if (bim_type_ == tag_bim_iptc_) {
	if (iptc_ == NULL) throw("IPTC is null in write");
	iRV = iptc_->Write(pFile);
	length += iRV;
	printf("Wrote tag_bim_iptc_ %d\n", iRV);
      } else {
	iRV = fwrite(&data_[0], sizeof(unsigned char),
		     bim_length_rounded, pFile);
	length += iRV;
	printf("Wrote BIM block %d", iRV);
      }
      return length;
    }

    unsigned short type() const { return bim_type_;}
    unsigned char pascalstringlength_;
    unsigned short bim_type_;
    vector<unsigned char> pascalstring_;
    unsigned int bim_length_;
    vector<unsigned char> data_;
    Iptc *iptc_;
    unsigned int magic_;
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
      int iRV = fwrite(headerstring_.c_str(), sizeof(unsigned char),
		       headerlen + 1, pFile);
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
