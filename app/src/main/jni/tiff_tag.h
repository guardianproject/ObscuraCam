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
 class MakerNote;
class TiffTag
{
public:
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
  TiffTag(FILE *pFile, bool byte_swapping);
  // Create a tag from raw data.
  TiffTag(int tag, enum tag_types type, int count, unsigned char *data);
  ~TiffTag();
  int GetDataLength() const;
  void TraceValue(int maxvals) const;
  void Print() const;
  TiffIfd *GetExif() {
    throw("Not implemented yet");
  }
  void SetValOut(unsigned int val);
  // Return the name of the given type.
  static const char *TypeName(tag_types t);
  // Return the name of the given type.
  static const char *GPSTagName(int tag);
  static const char *TagName(int tag);
  enum gps_tag_names {
    gps_version = 0x0,
    gps_lat_ref = 0x1,
    gps_lat = 0x2,
    gps_long_ref = 0x3,
    gps_long = 0x4,
    gps_alt_ref = 0x5,
    gps_alt = 0x6,
    gps_time_stamp = 0x7,
    gps_satellites = 0x8,
    gps_status = 0x9,
    gps_measure_mode = 0xa,
    gps_dop = 0xb,
    gps_speed_ref = 0xc,
    gps_speed = 0xd,
    gps_track_ref = 0xe,
    gps_track = 0xf,
    gps_img_direction_ref = 0x10,
    gps_img_direction = 0x11,
    gps_map_datum = 0x12,
    gps_dest_lat_ref = 0x13,
    gps_dest_lat = 0x14,
    gps_dest_long_ref = 0x15,
    gps_dest_long = 0x16,
    gps_dest_bearing_ref = 0x17,
    gps_dest_bearing = 0x18,
    gps_dest_dist_ref = 0x19,
    gps_dest_dist = 0x1a,
  };
  enum tag_names {
    tag_ImageWidth = 0x100,
    tag_ImageHeight=0x101,
    tag_BitsPerSample = 0x102,
    tag_Compression = 0x103,
    tag_PhotometricInterpretation = 0x106,
    tag_Title = 0x10e,
    tag_Make = 0x10f,
    tag_Model = 0x110,
    tag_StripOffsets = 0x111,
    tag_Orientation = 0x112,
    tag_SamplesPerPixel = 0x115,
    tag_RowsPerStrip = 0x116,
    tag_StripByteCounts = 0x117,
    tag_XResolution = 0x11A,
    tag_YResolution = 0x11B,
    tag_PlanarConfiguration = 0x11C,
    tag_ResolutionUnit = 0x128, // 2= inches 3=centimeters
    tag_TransferFunction = 0x12D,
    tag_Software = 0x131,
    tag_DateChange = 0x132,
    tag_Artist = 0x13B,
    tag_WhitePoint = 0x13E,
    tag_PrimaryChromaticities = 0x13F,
    tag_ThumbnailOffset = 0x201,
    tag_ThumbnailLength = 0x202,
    tag_YCbCrCoefficients = 0x211,
    tag_YCbCrSubSampling = 0x212,
    tag_YCbCrPositioning = 0x213,
    tag_ReferenceBlackWhite = 0x214,
    tag_Copyright = 0x8298,
    tag_ExposureTime = 0x829A,
    tag_FNumber = 0x829D,
    tag_ExposureProgram = 0x8822,
    tag_ExifIFDPointer = 0x8769,
    tag_SpectralSensitivity = 0x8824,
    tag_GpsInfoIFDPointer = 0x8825,
    tag_ISOSpeedRatings = 0x8827,
    tag_OECF = 0x8828,

    tag_ExifVersion = 0x9000,
    tag_DateTimeOriginal = 0x9003,
    tag_DateTimeDigitized = 0x9004,

    tag_ShutterSpeedValue = 0x9201,
    tag_ApertureValue = 0x9202,
    tag_BrightnessValue = 0x9203,
    tag_ExposureBiasValue = 0x9204,
    tag_MaxApertureValue = 0x9205,
    tag_SubjectDistance = 0x9206,
    tag_MeteringMode = 0x9207,
    tag_LightSource = 0x9208,
    tag_Flash = 0x9209,
    tag_FocalLength = 0x920A,
    tag_MakerNote = 0x927c ,

    tag_Interoperability = 0xa005
  };

  // Must call WriteDataBlock first to set valpointerout_
  // and write the data block if appropriate.
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
      if ((( int*)data_)[pos*2] == 0)
	return 0;
      return(((unsigned int*)data_)[pos*2] /
	     (double)((unsigned int*)data_)[pos*2+1]);
    case tiff_rational:
      if ((( int*)data_)[pos*2] == 0)
	return 0;
      return((( int*)data_)[pos*2] /
	     (double)(( int*)data_)[pos*2+1]);
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
  void SetStringValue(const char *s) {
    if (type_ != tiff_string)
      throw("SetStringValue on non-string.");
    int totallength = strlen(s) + 1;
    count_ = totallength;
    delete [] data_;
    data_ = new unsigned char [totallength];
    strncpy((char*)data_, s, totallength);
  }
  static int LengthOfType(short type) {
     if (type == tiff_int8 || type == tiff_uint8 ||
	 type == tiff_string|| type == tiff_bytes)
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
  MakerNote *makernote_;
private:
  bool TagIsSubIFD() const;
};
}  // namespace jpeg_redaction
#endif // INCLUDE_TIFF_TAG
