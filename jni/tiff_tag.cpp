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


// tiff_tag.cpp: implementation of the TiffTag class.
//
//////////////////////////////////////////////////////////////////////

#include "debug_flag.h"
#include "tiff_tag.h"
#include "tiff_ifd.h"
#include "byte_swapping.h"
#include "makernote.h"

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////

namespace jpeg_redaction {
  TiffTag::TiffTag(FILE *pFile, bool byte_swapping) : 
    data_(NULL), subifd_(NULL), makernote_(NULL) {
    if (pFile == NULL)
      throw("NULL file");
    int iRV = fread(&tagid_, sizeof(short), 1, pFile);
    if (iRV != 1) throw("Can't read file");
    if (byte_swapping)
      ByteSwapInPlace(&tagid_, 1);
    iRV = fread(&type_, sizeof(short), 1, pFile);
    if (iRV != 1) throw("Can't read file");
    if (byte_swapping)
      ByteSwapInPlace(&type_, 1);
    iRV = fread(&count_, sizeof(unsigned int), 1, pFile);
    if (iRV != 1) throw("Can't read file");
    if (byte_swapping)
      ByteSwapInPlace(&count_, 1);

    unsigned int value = 0;
    iRV = fread(&value, sizeof(unsigned int), 1, pFile);
    if (iRV != 1) throw("Can't read file");
    int totallength = GetDataLength();
    if ((totallength > 4 || TagIsSubIFD()) && byte_swapping) {
      //      printf("Swapping pointer\n");// If it's a pointer.
      ByteSwapInPlace(&value, 1);
    }
    if (totallength < 0 || totallength > 1e8) {
      fprintf(stderr, "tag %d totallength %d", tagid_, totallength);
      throw("totallength is broken");
    }
    // Some types are pointers that will be stored in an ifd.
    if (totallength <= 4 && ! (TagIsSubIFD()) ) {
      if (byte_swapping) {
	//	printf("Swapping non-pointer\n");// If it's a pointer.
        ByteSwapInPlace((unsigned char *)&value,
                        4/LengthOfType(type_), LengthOfType(type_));
      }
      loaded_ = true;
      data_ = new unsigned char [totallength];
      memcpy(data_, &value, totallength);
      valpointer_ = 0;
    } else {
      loaded_ = false;
      valpointer_ = value;
      // Too long to fit in the tag.
    }
    if (debug > 1)
      printf("Read tag %d/0x%x, type %s count %d value %d/0x%x "
	     "bytes %d\n",
	     tagid_, tagid_, TypeName((tag_types)type_), count_,
	     value, value, totallength);
  }
  TiffTag::TiffTag(int tagid, enum tag_types type, int count,
		   unsigned char *data) : makernote_(NULL) {
    tagid_ = tagid;
    type_ = type;
    count_ = count;
    int totallength = count * LengthOfType(type);
    data_ = new unsigned char[totallength];
    memcpy(data_, data, totallength);
    subifd_ = NULL;
    loaded_ = true;
  }

  const char *TiffTag::GPSTagName(const int tag) {
    switch (tag) {
    case gps_version: return "GPSversion";
    case gps_lat_ref: return "GPSlat_ref";
    case gps_lat: return "GPSlat";
    case gps_long_ref: return "GPSlong_ref";
    case gps_long: return "GPSlong";
    case gps_alt_ref: return "GPSalt_ref";
    case gps_alt: return "GPSalt";
    case gps_time_stamp: return "GPStime_stamp";
    case gps_satellites: return "GPSsatellites";
    case gps_status: return "GPSstatus";
    case gps_measure_mode: return "GPSmeasure_mode";
    case gps_dop: return "GPSdop";
    case gps_speed_ref: return "GPSspeed_ref";
    case gps_speed: return "GPSspeed";
    case gps_track_ref: return "GPStrack_ref";
    case gps_track: return "GPStrack";
    case gps_img_direction_ref: return "GPSimg_direction_ref";
    case gps_img_direction: return "GPSimg_direction";
    case gps_map_datum: return "GPSmap_datum";
    case gps_dest_lat_ref: return "GPSdest_lat_ref";
    case gps_dest_lat: return "GPSdest_lat";
    case gps_dest_long_ref: return "GPSdest_long_ref";
    case gps_dest_long: return "GPSdest_long";
    case gps_dest_bearing_ref: return "GPSdest_bearing_ref";
    case gps_dest_bearing: return "GPSdest_bearing";
    case gps_dest_dist_ref: return "GPSdest_dist_ref";
    case gps_dest_dist: return "GPSdest_dist";
      }
    return NULL;
}
  const char *TiffTag::TagName(const int tag) {
    switch (tag) {
    case tag_ImageWidth: return "ImageWidth";
    case tag_ImageHeight: return "ImageHeight";
    case tag_BitsPerSample: return "BitsPerSample";
    case tag_Compression: return "Compression";
    case tag_PhotometricInterpretation: return "PhotometricInterpretation";
    case tag_Title: return "Title";
    case tag_Make: return "Make";
    case tag_Model: return "Model";
    case tag_StripOffsets: return "StripOffsets";
    case tag_Orientation: return "Orientation";
    case tag_SamplesPerPixel: return "SamplesPerPixel";
    case tag_RowsPerStrip: return "RowsPerStrip";
    case tag_StripByteCounts: return "StripByteCounts";
    case tag_XResolution: return "XResolution";
    case tag_YResolution: return "YResolution";
    case tag_PlanarConfiguration: return "PlanarConfiguration";
    case tag_ResolutionUnit: return "ResolutionUnit";// 2= inches 3=centimeters
    case tag_TransferFunction: return "TransferFunction";
    case tag_Software: return "Software";
    case tag_DateChange: return "DateChange";
    case tag_Artist: return "Artist";
    case tag_WhitePoint: return "WhitePoint";
    case tag_PrimaryChromaticities: return "PrimaryChromaticities";
    case tag_ThumbnailOffset: return "ThumbnailOffset";
    case tag_ThumbnailLength: return "ThumbnailLength";
    case tag_YCbCrCoefficients: return "YCbCrCoefficients";
    case tag_YCbCrSubSampling: return "YCbCrSubSampling";
    case tag_YCbCrPositioning: return "YCbCrPositioning";
    case tag_ReferenceBlackWhite: return "ReferenceBlackWhite";
    case tag_Copyright: return "Copyright";
    case tag_ExposureTime: return "ExposureTime";
    case tag_FNumber: return "FNumber";
    case tag_ExposureProgram: return "ExposureProgram";
    case tag_ExifIFDPointer: return "ExifIFDPointer";
    case tag_SpectralSensitivity: return "SpectralSensitivity";
    case tag_GpsInfoIFDPointer: return "GpsInfoIFDPointer";
    case tag_ISOSpeedRatings: return "ISOSpeedRatings";
    case tag_OECF: return "OECF";

    case tag_ExifVersion: return "ExifVersion";
    case tag_DateTimeOriginal: return "DateTimeOriginal";
    case tag_DateTimeDigitized: return "DateTimeDigitized";

    case tag_ShutterSpeedValue: return "ShutterSpeedValue";
    case tag_ApertureValue: return "ApertureValue";
    case tag_BrightnessValue: return "BrightnessValue";
    case tag_ExposureBiasValue: return "ExposureBiasValue";
    case tag_MaxApertureValue: return "MaxApertureValue";
    case tag_SubjectDistance: return "SubjectDistance";
    case tag_MeteringMode: return "MeteringMode";
    case tag_LightSource: return "LightSource";
    case tag_Flash: return "Flash";
    case tag_FocalLength: return "FocalLength";
    case tag_MakerNote: return "MakerNote";

    case tag_Interoperability: return "Interoperability";
    }
    const char *gps_tag = GPSTagName(tag);
    if (gps_tag != NULL)
      return gps_tag;
    return "Unknown";
  }
  // Return the name of the given type.
  const char *TiffTag::TypeName(tag_types t) {
    switch (t) {
    case tiff_uint8 :
      return "uint8";
    case tiff_string:
      return "string";
    case tiff_uint16:
      return "uint16";
    case tiff_uint32 :
      return "uint32";
    case tiff_urational:
      return "urational";
    case tiff_int8:
      return "int8";
    case tiff_bytes:
      return "bytes";
    case tiff_int16:
      return "int16";
    case tiff_int32:
      return "int32";
    case tiff_rational:
      return "rational";
    case tiff_float:
      return "float";
    case tiff_double:
      return "double";
    default:
    case tiff_unknown:
      return "unknown";
    }
  }

TiffTag::~TiffTag() {
    delete subifd_;
    delete [] data_;
}

// Write out return 0 if the tag contains the data.
// If a pointer must be written, write a dummy, but return the offset
// so it can be corrected later.
int TiffTag::Write(FILE *pFile) const {
  int pointer_location = 0;
  int iRV = fwrite(&tagid_, sizeof(short), 1, pFile);
  if (iRV != 1) throw("Can't write file");
  iRV = fwrite(&type_, sizeof(short), 1, pFile);
  if (iRV != 1) throw("Can't write file");
  iRV = fwrite(&count_, sizeof(unsigned int), 1, pFile);
  if (iRV != 1) throw("Can't write file");

  const int totallength = GetDataLength();
  pointer_location = ftell(pFile);
  // Pad with zeros so output is consistent even for outputs < 4 bytes.
  unsigned char raw[4] = {0,0,0,0};
  if (totallength <= 4 && data_)
    memcpy(raw, data_, totallength);
  iRV = fwrite(&raw, sizeof(unsigned char), 4, pFile);
  if (iRV != 4) throw("Can't write file");
  // Return where the pointer has to be written.
  if (totallength > 4 || TagIsSubIFD() ||
      tagid_ == tag_StripOffsets || tagid_ == tag_ThumbnailOffset ||
      tagid_ == tag_StripByteCounts || tagid_ == tag_ThumbnailLength) {
    return pointer_location;
  } else {
    return 0;
  }
}

// Write out datablocks, returning the pointer. Return if no datablock.
int TiffTag::WriteDataBlock(FILE *pFile, int subfileoffset) {
  int totallength = count_ * LengthOfType(type_);
  //  int subfileoffset = 0; // TODO(aws) should this be non-zero?
  if (TagIsSubIFD()) {
    unsigned int zero = 0;
    if (subifd_ == NULL)
      throw("subifd is nul in TiffTag::WriteDataBlock");
    valpointerout_ = subifd_->Write(pFile, zero, subfileoffset);
    return valpointerout_; // Will get subtracted later.
  }
  if (tagid_ == tag_MakerNote) {
    if (makernote_ != NULL) {
      valpointerout_ = ftell(pFile);
      makernote_->Write(pFile, 0);
      return valpointerout_;
    }
  }
  if (totallength > 4) {
    if (!loaded_)
      throw("Trying to write when data was never read");
    valpointerout_ = ftell(pFile);
    fwrite(data_, sizeof(unsigned char), totallength, pFile);
    return valpointerout_;
  }

  return 0;
}

// Load a type that didn't fit in the 4 bytes
int TiffTag::Load(FILE *pFile, unsigned int subfileoffset,
		   bool byte_swapping) {
  if (loaded_)
    return 0;
  if (pFile == NULL)
    throw("NULL file");
  int position = valpointer_ + subfileoffset;
  if (TagIsSubIFD()) {
    // IFDs use absolute position, normal tags are relative to subfileoffset.
    //if (tagid_ == tag_MakerNote)
    //    position = valpointer_;
    if (debug > 0)
      printf("Loading SUB IFD 0x%x at %d (%d + %d) ", tagid_, position,
	     valpointer_, subfileoffset);

    subifd_ = new TiffIfd(pFile, position, true,
			    subfileoffset, byte_swapping);
    loaded_ = true;
    return 1;
  }
  int iRV = fseek(pFile, position, SEEK_SET);
  if (tagid_ == tag_MakerNote) {
    printf("Reading Makernote\n");
    MakerNoteFactory factory;
    makernote_ = factory.Read(pFile, subfileoffset, count_);
    if (makernote_ != NULL) {
      loaded_ = true;
      return count_;
    } else {
      fprintf(stderr, "Failed to read Makernote.");
      // Otherwise fall through to general handling.
    }
    //    position = valpointer_;
  }
  const int type_len = LengthOfType(type_);
  const int totallength = count_ * type_len;
  data_ = new unsigned char [totallength];
  iRV = fread(data_, sizeof(char), totallength, pFile);
  if (iRV  != totallength)
    throw("Couldn't read data block.");
  if (byte_swapping) {
    if (type_ == tiff_rational || type_ == tiff_urational)
      ByteSwapInPlace(data_, count_ * 2, type_len/2);
    else
      ByteSwapInPlace(data_, count_, type_len);
  }
  loaded_ = true;
  return totallength;
}

void TiffTag::SetValOut(unsigned int val) {
  valpointerout_ = val;
}

bool TiffTag::TagIsSubIFD() const {
  return(tagid_ == tag_ExifIFDPointer || tagid_ == tag_GpsInfoIFDPointer ||
         /* tagid_ == tag_MakerNote || */ tagid_ == tag_Interoperability);
}

void TiffTag::Print() const {
  printf("0x%04x %-17s %2dx%d %-10s (", tagid_, TagName(tagid_), count_,
	 LengthOfType(type_), TypeName((tag_types)type_));
  TraceValue(4);
  printf(") ");
}

void TiffTag::TraceValue(int maxvals) const {
  if (TagIsSubIFD()) {
    printf("IFD %d", valpointer_);
    return;
  }
  if (tagid_ == tag_MakerNote) {
    if (makernote_) {
      printf("Now printing makernote\n");
      makernote_->Print();
    } else
      printf("unresolved makernote...");
    return;
  }
  for(int i=0; i<maxvals && i< count_; ++i) {
    switch(type_) {
    case tiff_string:
      if (loaded_)
	printf("\"%s\"", GetStringValue());
      else
	printf("\"NOT LOADED\"");
      return;

    case tiff_float:
    case tiff_double:
    case tiff_rational:
    case tiff_urational:
      if (loaded_)
	printf("%f", GetFloatValue(i));
      else
	printf("Float not loaded");
      break;
    case tiff_bytes:
      if (loaded_)
	printf("0x%02x", GetUIntValue(i));
      else
	printf("bytes not loaded");
      break;
    case tiff_uint8:
    case tiff_uint16:
    case tiff_uint32:
      if (loaded_)
	printf("%u", GetUIntValue(i));
      else
	printf("Uint not loaded");
      break;
    case tiff_int8:
    case tiff_int16:
    case tiff_int32:
      if (loaded_)
	printf("%d", GetIntValue(i));
      else
	printf("int not loaded");
      break;
    }
    if (i<count_-1)
      printf(" ");
  }
}

int TiffTag::GetDataLength() const
{
  return (count_ * LengthOfType(type_));
}
}  // namespace jpeg_redaction
