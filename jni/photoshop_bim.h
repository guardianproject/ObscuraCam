// photoshop_bim.h: interface for the PhotoshopBIM class.
//
//////////////////////////////////////////////////////////////////////

#ifndef INCLUDE_PHOTOSHOP_BIM
#define INCLUDE_PHOTOSHOP_BIM

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include <vector>
class Iptc;

class PhotoshopBIM {
public:
  PhotoshopBIM(FILE *pFile);
  virtual ~PhotoshopBIM();
  int Write(FILE *pFIle);
protected:
  unsigned short type() const { return bim_type_;}
  unsigned char pascalstringlength_;
  unsigned short bim_tag_; // TODO(AWS) check this.
  unsigned short bim_type_;
  vector<unsigned char> pascalstring_;
  unsigned int bim_length_;
  vector<unsigned char> data_;
  Iptc *iptc_;
};

#endif // INCLUDE_PHOTOSHOP_BIM
