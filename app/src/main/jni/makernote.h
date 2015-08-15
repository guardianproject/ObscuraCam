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

// makernote.h
// classes for reading, writing & accessing makernote sections of
// EXIF files. Currently only some of these are partially implemented.

// Makernotes for various manufacturers are described in 
// http://www.ozhiker.com/electronics/pjmt/jpeg_info/makernotes.html

#ifndef JPEG_REDACTION_LIB_MAKERNOTE
#define  JPEG_REDACTION_LIB_MAKERNOTE

#include <stdio.h>
#include <string>
#include <vector>
#include "tiff_ifd.h"
#include "debug_flag.h"

namespace jpeg_redaction {

  class MakerNote {
  public:
    MakerNote() {}
    virtual void Print() const = 0;
    virtual int Read(FILE *pFile, int subfileoffset, int length) = 0;
    virtual int Write(FILE *pFile, int subfileoffset) const = 0;
  };

  // A Generic Makernote where we just read a block of data.
  // without parsing, hoping that it is relocatable.
  class GenericMakerNote : public MakerNote {
  public:
  GenericMakerNote() {}
    ~GenericMakerNote() {}
    virtual void Print() const {
      printf("Generic makernote length %zu\n", data_.size());
    }
    virtual int Read(FILE *pFile, int subfileoffset, int length) {
      data_.resize(length);
      int iRV = fread(&data_.front(), sizeof(unsigned char), length, pFile);
      if (iRV != length) {
	data_.clear();
	return 0;
      }
      return 1;
    };
    virtual int Write(FILE *pFile, int subfileoffset) const {
      int iRV = fwrite(&data_.front(), sizeof(unsigned char),
		       data_.size(), pFile);
      if (iRV != data_.size()) {
	return 0;
      }
      return 1;
    };
    std::vector<unsigned char> data_;
  };

  // A makernote stored in a standard TiffIfd, e.g. Canon.
  class IfdMakerNote : public MakerNote {
  public:
    IfdMakerNote() {}
    virtual void Print() const {
      printf("IDF makernote...\n");
      ifd_->Print();
    }
    virtual int Read(FILE *pFile, int subfileoffset, int length) {};
    virtual int Write(FILE *pFile, int subfileoffset) const {};
  protected:
    TiffIfd *ifd_;
  };

  class Panasonic: public MakerNote {
  public:
  Panasonic() : ifd_(NULL) {}
    ~Panasonic() {
      delete ifd_;
      ifd_ = NULL;
    }
    virtual void Print() const {
      if (debug > 1)
	printf("Panasonic makernote... %p\n", this);
      if (ifd_ == NULL)
	throw("Panasonic ifd is NULL");
      ifd_->Print();
    }
    virtual int Read(FILE *pFile, int subfileoffset, int length) {
      int start = ftell(pFile);
      char header[12];
      int iRV = fread(header, sizeof(char), 12, pFile);
      if (strcmp(header, "Panasonic") != 0) {
	fseek(pFile, start, SEEK_SET);
	return 0;
      }
      printf("Loading Panasonic\n");
      ifd_ = new TiffIfd(pFile, start+12, true, subfileoffset);
      TiffTag *tag;
      tag = ifd_->FindTag(0x69);
      if (tag) printf("Panasonic69: %s\n", (const char *)tag->GetData());
      tag = ifd_->FindTag(0x6b);
      if (tag) printf("Panasonic6b: %s\n", (const char *)tag->GetData());
      tag = ifd_->FindTag(0x6d);
      if (tag) printf("Panasonic6d: %s\n", (const char *)tag->GetData());
      tag = ifd_->FindTag(0x6f);
      if (tag) printf("Panasonic6f: %s\n", (const char *)tag->GetData());

      fseek(pFile, start, SEEK_SET);
      return 1;
    }
    virtual int Write(FILE *pFile, int subfileoffset) const {
      if (ifd_ == NULL) throw("Trying to save NULL Panasonic makernote");
      fwrite("Panasonic\0\0\0", sizeof(char), 12, pFile);
      unsigned int urv = ifd_->Write(pFile, 0, subfileoffset);
      return 1;
    }
  protected:
    TiffIfd *ifd_;
  };

  class MakerNoteFactory {
  public:
    MakerNoteFactory() {}
    MakerNote *Read(FILE *pFile, int subfileoffset, int length) {
      int rv;
      size_t start_location = ftell(pFile);
      /* IfdMakerNote *ifdmn = new IfdMakerNote; */
      /* rv = ifdmn->Read(pFile, subfileoffset, length); */
      /* if (rv == 1) */
      /* 	return ifdmn; */
      /* delete ifdmn; */
      /* rv = fseek(pFile, start_location, SEEK_SET); */

      Panasonic *panasonic = new Panasonic;
      rv = panasonic->Read(pFile, subfileoffset, length);
      if (rv == 1)
      	return panasonic;
      delete panasonic;
      rv = fseek(pFile, start_location, SEEK_SET);
      GenericMakerNote *generic = new GenericMakerNote;
      rv = generic->Read(pFile, subfileoffset, length);
      if (rv == 1)
	return generic;
      delete generic;
      return NULL;
    }
  };

}  // namespace jpeg_redaction
#endif // JPEG_REDACTION_LIB_MAKERNOTE
