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


// tiff_tag.h: interface for the TiffTag class.
//
//////////////////////////////////////////////////////////////////////

#ifndef INCLUDE_TIFF_TAG
#define INCLUDE_TIFF_TAG

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// 0x8769 EXIF
// 0x8825  GPS
// 0x927c Makernote

namespace jpeg_redaction {
class TiffIfd;
class TiffTag
{
public:
  int GetDataLength() const;
  void TraceValue(int maxvals) const;
  void Print() const;
  TiffIfd *GetExif() {
    throw("Not implemented yet");
  }
  void SetValOut(unsigned int val);
  enum tag_types {tiff_unknown=0,
		  tiff_uint8 = 1,
		  tiff_string=2,
		  tiff_uint16=3,
		  tiff_uint32 =4,
		  tiff_urational=5,
		  tiff_int8=6,
		  tiff_bytes=7,
		  tiff_int16=8,
		  tiff_int32=9,
		  tiff_rational=10,
		  tiff_float=11,
		  tiff_double=12};
  enum tag_names {tag_width = 0x100,
		  tag_height=0x101,
		  tag_bitspersample = 0x102,
		  tag_compression = 0x103,
		  tag_make = 0x10f,
		  tag_model = 0x110,
		  tag_stripoffset = 0x111,
		  tag_orientation = 0x112,
		  tag_stripbytes = 0x117,
		  tag_xresolution = 0x11A,
		  tag_yresolution = 0x11B,
		  tag_resolutionunit = 0x128, // 2= inches 3=centimeters
		  tag_datechange = 0x132,
		  tag_YCbCrPositioning = 0x213,
		  tag_thumbnailoffset = 0x201,
		  tag_thumbnaillength = 0x202,
		  tag_exif = 0x8769,
		  tag_gps = 0x8825,
		  tag_makernote = 0x927c ,
		  tag_interoperability = 0xa005};
  ~TiffTag();
  TiffTag(FILE *pFile, bool byte_swapping);

  // Must call WriteDataBlock first to set valpointerout_ and write the data block if appropriate.
  int Write(FILE *pFile) const;
  // Call this first to write out the data block and set the valpointerout_
  int WriteDataBlock(FILE *pFile, int subfileoffset);

  // Load a type that didn't fit in the 4 bytes
  int Load(FILE *pFile, unsigned int subfileoffset, bool byte_swapping);
  tag_types GetType() const { return (tag_types)type_; }
  int GetTag() const { return tagid_; }
  int GetCount() const { return count_; }
  double GetFloatValue(unsigned int pos = 0) const {
    if (!loaded_) throw("Not loaded");
    if (!data_) throw("No data");
    if (pos >= count_)
      throw("Index too high");
    switch (type_) {
    case tiff_float:
      return(((float*)data_)[pos]);
    case tiff_double:
      return(((double*)data_)[pos]);
    case tiff_urational:
      return(((unsigned int*)data_)[pos*2] / (double)((unsigned int*)data_)[pos*2+1]);
    case tiff_rational:
      return((( int*)data_)[pos*2] / (double)(( int*)data_)[pos*2+1]);
    }
    throw("wrong type not float/double/rational");
  }

  unsigned int GetUIntValue(unsigned int pos) const {
    if (!loaded_) throw("Not loaded");
    if (!data_) throw("No data");
    if (pos >= count_)
      throw("Index too high");
    switch (type_) {
    case tiff_uint8:
    case tiff_bytes:
      return(((unsigned char*)data_)[pos]);
    case tiff_uint16:
      return(((unsigned short*)data_)[pos]);
    case tiff_uint32:
      return(((unsigned int*)data_)[pos]);
    }
    throw("wrong type not UInt");
  }
  int GetIntValue(unsigned int pos) const {
    if (!loaded_) throw("Not loaded");
    if (!data_) throw("No data");
    if (pos >= count_)
      throw("Index too high");
    switch (type_) {
    case tiff_int8:
      return((( char*)data_)[pos]);
    case tiff_int16:
      return((( short*)data_)[pos]);
    case tiff_int32:
      return((( int*)data_)[pos]);
    }
    throw("wrong type not Int");
  }
  const char * GetStringValue() const {
    if (!loaded_) throw("Not loaded");
    if (!data_) throw("No data");
    if (type_!= tiff_string)
      throw("Not a string");
      return((const char *)data_);
  }
  const unsigned char * GetData() const {
      return(data_);
  }

  static int LengthOfType(short type) {
     if (type == tiff_int8|| type == tiff_uint8|| type == tiff_string|| type == tiff_bytes)
       return 1;
     if (type == tiff_uint16 || type == tiff_int16)
       return 2;
     if (type == tiff_double || type == tiff_rational || type == tiff_urational)
       return 8;
     if (type > tiff_double || type <=0)
       return -1;
     return 4;
  }

protected:
  unsigned short tagid_;
  unsigned short type_;
  unsigned int count_;

  bool loaded_;
  unsigned char *data_;
  unsigned int valpointer_;
  mutable unsigned int valpointerout_;
  TiffIfd *subifd_; // Makernote or Exif
private:
  bool TagIsSubIFD() const;
};
}  // namespace jpeg_redaction
#endif // INCLUDE_TIFF_TAG
