// PhotoshopBIM.cpp: implementation of the PhotoshopBIM class.
//
//////////////////////////////////////////////////////////////////////

#include "photoshop_bim.h"
#include "iptc.h"

#define tag_bim_iptc_ 0x0404 // The bim type for IPTC data

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////

PhotoshopBIM::PhotoshopBIM(FILE *pFile)
{
  
  int iRV = fread(&bim_type_, sizeof(unsigned short), 1, pFile);
  
  iRV = fread(&pascalstringlength_, sizeof(unsigned char), 1, pFile);
  
  unsigned char pascalstringlengthrounded = pascalstringlength_ + ((pascalstringlength_==0)?1:0);
  pascalstring_.resize(pascalstringlengthrounded);
  iRV = fread(&pascalstring_[0], sizeof(unsigned char), pascalstringlengthrounded, pFile);

  iRV = fread(&bim_length_, sizeof(unsigned int), 1, pFile);

  unsigned int bim_length_rounded = bim_length_ + (bim_length_%2);
  if (bim_type_ == tag_bim_iptc_) {
    iptc_ = new CIptc(pFile);
  } else {
    data_.resize(bim_length_rounded);
    iRV = fread(&data_[0], sizeof(unsigned char), bim_length_rounded, pFile);
  }
  throw("Got an unsupported BIM");
}

PhotoshopBIM::~PhotoshopBIM()
{

}

int PhotoshopBIM::Write(FILE *pFile)
{
  int length = 0;
  int iRV;
  iRV = fwrite(&bim_tag_, sizeof(unsigned int), 1, pFile);

  iRV = fwrite(&bim_type_, sizeof(unsigned short), 1, pFile);
  
  iRV = fwrite(&pascalstringlength_, sizeof(unsigned char), 1, pFile);
  
  unsigned char pascalstringlengthrounded = pascalstringlength_ + ((pascalstringlength_==0)?1:0);
  pascalstring_.resize(pascalstringlengthrounded);
  iRV = fwrite(&pascalstring_[0], sizeof(unsigned char), pascalstringlengthrounded, pFile);

  unsigned int bim_length_rounded = bim_length_ + (bim_length_%2);
  iRV = fwrite(&bim_length_rounded, sizeof(unsigned int), 1, pFile);
  iRV = fwrite(&data_[0], sizeof(unsigned char), bim_length_rounded, pFile);
  
  return length;
}
