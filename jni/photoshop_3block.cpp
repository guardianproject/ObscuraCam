// photoshop_3block.cpp: implementation of the Photoshop3Block class.
//
//////////////////////////////////////////////////////////////////////

#include "photoshop_3block.h"
#include "PhotoshopBIM.h"

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////

namespace jpeg_redaction {
Photoshop3Block::Photoshop3Block(FILE *pFile)
{
  char c;
  while((c=fgetc(pFile))!='\0'){
    headerstring_ += c;
  }
  unsigned int magic;
  int iRV = fread(&magic, sizeof(unsigned int), 1, pFile);

  if (magic != tag_bim_)
    printf("BIM was 0x%x not 0x %x\n", magic, tag_bim_);
  PhotoshopBIM *bim = new PhotoshopBIM(pFile);
  bims_.push_back(bim);
}

Photoshop3Block::~Photoshop3Block()
{
  for(int i = 0; i < bims_.size(); ++i) {
    delete bims_[i];
  }
  bims_.clear();
}

int Photoshop3Block::Write(FILE *pFile)
{
  int length = 0;
  int headerlen = headerstring_.length();
  int iRV = fwrite(headerstring_.c_str(), sizeof(unsigned char), headerlen + 1, pFile);
  if (iRV != headerlen + 1)
    throw("Can't write");
  length += iRV;

  for (int i = 0; i < tags_.size(); ++i) {
    iRV = tags_[i]->Write(pFile);
    length += iRV;
  }
  return length;
}
}  // namespace jpeg_redaction
