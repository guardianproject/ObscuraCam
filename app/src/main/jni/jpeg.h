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


// jpeg.h: interface for the Jpeg class.
//
//////////////////////////////////////////////////////////////////////

#ifndef INCLUDE_JPEG
#define INCLUDE_JPEG

#include <vector>
#include <string>
#include <stdlib.h>
#include <stdio.h>
#include "tiff_ifd.h"
#include "obscura_metadata.h"

namespace jpeg_redaction {

class Iptc;
class JpegDHT;
class JpegMarker;
class Redaction;

class Photoshop3Block;
class Jpeg {
public:
  enum markers { jpeg_soi = 0xFFD8,
		 jpeg_sof0 = 0xFFC0,
		 jpeg_sof2 = 0xFFC2,
		 jpeg_dht = 0xFFC4,
		 jpeg_eoi = 0xFFD9,
		 jpeg_sos = 0xFFDA,
		 jpeg_dqt = 0xFFDB,
		 jpeg_dri = 0xFFDD,
		 jpeg_com = 0xFFDE,
		 jpeg_app = JPEG_APP0};
  class JpegComponent {
  public:
    JpegComponent(unsigned char *d) {
      id_ = d[0];
      v_factor_ = d[1] & 0xf;
      h_factor_ = d[1] >> 4;
      table_ = d[2];
    }
    void Print() {
      printf("Component %d H %d V %d, Table %d\n",
	     id_, h_factor_, v_factor_, table_);
    }
    int id_;
    int h_factor_;
    int v_factor_;
    int table_;
  };
  // Trivial constructor.
  Jpeg() : filename_(""), width_(0), height_(0), photoshop3_(NULL) {};
  virtual ~Jpeg();
  // Construct from a file. loadall indicates whether to load all
  // data blocks, or just parse the file and extract metadata.
  bool LoadFromFile(char const * const pczFilename, bool loadall);
  int LoadFromFile(FILE *pFile, bool loadall, int offset);

  // Parse the JPEG data and redact if Redaction regions are supplied.
  // If pgm_save_filename is provided, write the decoded image to that file.

  void DecodeImage(Redaction *redaction, const char *pgm_save_filename);
  // Invert the redaction by pasting in the strips from redaction.
  int ReverseRedaction(const Redaction &redaction);
  int GetHeight() const { return height_; }
  int GetWidth() const { return width_; }
  Jpeg *GetThumbnail();
  int RedactThumbnail(Redaction *redaction);
  // Save the current (possibly redacted) version of the JPEG out.
  // Return 0 on success.
  int Save(const char * const filename);
  int Save(FILE *pFile);

  const char *MarkerName(int marker) const;
  Iptc *GetIptc();
  // If there is one, return the first IFD.
  TiffIfd *GetIFD() {
    if (ifds_.size() >= 1)
      return ifds_[0];
    return NULL;
  }
  // Set the obscura metadata block, deleting any previous data.
  void SetObscuraMetaData(unsigned int length,
			  const unsigned char *data) {
    obscura_metadata_.SetDescriptor(length, data);
  }
  // Find the metadata if any.
  const unsigned char *GetObscuraMetaData(unsigned int *length) const {
    return obscura_metadata_.GetDescriptor(length);
  }
  // Return a pointer to the Exif IFD if present. 
  ExifIfd *GetExif() {
    if (ifds_.size() >= 1)
      return ifds_[0]->GetExif();
    // for (int i=0; i < ifds_.size(); ++i)
    //   if (ifds_[i]->GetExif())
    // 	return ifds_[i]->GetExif();
    return NULL;
  }
  
  TiffTag *FindTag(int tag_num) {
    for (int i = 0; i < ifds_.size(); ++i) {
      TiffTag *tag = ifds_[i]->FindTag(tag_num);
      if (tag) return tag;
    }
  }
  int RemoveTag(int tag) {
    int removed = 0;
    for (int i = 0; i < ifds_.size(); ++i) {
      bool removed_this = ifds_[i]->RemoveTag(tag);
      if (removed_this) ++removed;
    }
    return removed;
  }
  // Remove the whole IPTC record.
  int RemoveIPTC();
  // Add here a list of all the tags that are considered sensitive.
  int RemoveAllSensitive() {
    RemoveTag(TiffTag::tag_ExifIFDPointer);
    RemoveTag(TiffTag::tag_GpsInfoIFDPointer);
    RemoveTag(TiffTag::tag_MakerNote);
    RemoveTag(TiffTag::tag_Make);
    RemoveTag(TiffTag::tag_Model);
    RemoveIPTC();
    // e.g. times, owner
  }

  // Return a pointer to the marker of the given type.
  JpegMarker *GetMarker(int marker);
  // If it's a SOF or SOS we pass a slice.
  JpegMarker *AddSOMarker(int location, int length,
			  FILE *pFile, bool loadall, int slice);
  // The length is the length from the file, including the storage for length.
  JpegMarker *AddMarker(int marker, int location, int length,
                        FILE *pFile, bool loadall);
protected:
  // After loading an SO Marker, remove the stuff bytes so the bitstream
  // can be read more easily.
  void RemoveStuffBytes();
  void BuildDHTs(const JpegMarker *dht_block);
  int ReadSOSMarker(FILE *pFile, unsigned int blockloc, bool loadall);
  int LoadExif(FILE *pFile, unsigned int blockloc, bool loadall);

  std::vector<JpegMarker*> markers_;
  int width_;
  int height_;
  int softype_;
  std::vector<TiffIfd *> ifds_;
  std::string filename_;
  //  unsigned int datalen_, dataloc_;

  unsigned int restartinterval_;

  Photoshop3Block *photoshop3_;
  std::vector<JpegDHT*> dhts_;
  std::vector<JpegComponent*> components_;
  ObscuraMetadata obscura_metadata_;
};  // Jpeg
}  // namespace redaction

#endif // INCLUDE_JPEG
