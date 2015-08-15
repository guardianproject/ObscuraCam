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


// tiff_ifd.h: interface for the TiffIfd class.

#ifndef JPEG_REDACTION_LIB_TIFF_IFD
#define JPEG_REDACTION_LIB_TIFF_IFD

#include <vector>

#include "tiff_tag.h"

namespace jpeg_redaction {
class TiffTag;
class Jpeg;
class ExifIfd;

class TiffIfd
{
public:
  TiffIfd(FILE *pFile, unsigned int ifdoffset, bool loadall = false,
	    unsigned int subfileoffset=0, bool byte_swapping = false);
  TiffIfd() {  }
  virtual ~TiffIfd() {
    Reset();
  }

  int AddTag(TiffTag *tag, bool allowmultiple);
  int LoadImageData(FILE *pFile, bool loadall);
  unsigned int Write(FILE *pFile,
		     unsigned int nextifdoffset,
		     int subfileoffset) const;
  ExifIfd *GetExif() { return (ExifIfd*)FindTag(TiffTag::tag_ExifIFDPointer); }

    // Find a tag with a particular number. Return -1 if not in this IFD.
  TiffTag *FindTag(const int tagno) const {
    const int tagindex = FindTagIndex(tagno);
    return (tagindex < 0) ? NULL : tags_[tagindex];
  }

  int FindTagIndex(const int tagno) const {
    for(int tagindex=0 ; tagindex < tags_.size(); ++tagindex) {
      if (tags_[tagindex]->GetTag() == tagno)
        return tagindex;
    }
    return -1;
  }

  // Remove one tag of type tagno. If all is specified delete all of them
  // e.g. for keywords.
  bool RemoveTag(const int tagno, bool all = false) {
    bool found = false;
    for(int tagindex=0 ; tagindex < tags_.size(); ++tagindex) {
      if (tags_[tagindex]->GetTag() == tagno) {
        delete tags_[tagindex];
	tags_.erase(tags_.begin() + tagindex);
	found = true;
	if (!all)
	  return found;
      }
    }
    return found;
  }

  // For anything referenced by a pointer, then seek to it and load it
  // into data_blocks_
  int LoadAll(FILE *pFile);
  int GetNextIfdOffset() const {return nextifdoffset_;}

  TiffTag *GetTag(int tagindex) const {
    if (tagindex < 0 || tagindex >= tags_.size())
      return NULL;
    return(tags_[tagindex]);
  }
  size_t GetNTags() const { return tags_.size(); }
  // Print all tags to stdout.
  void Print() const;
  Jpeg *GetJpeg() { return jpeg_;}
protected:
  void Reset();

  bool byte_swapping_;
  unsigned int nextifdoffset_;
  std::vector<TiffTag *> tags_;
  std::vector<unsigned char> data_;
  unsigned int subfileoffset_;
  Jpeg *jpeg_;
};

// Subclass if it is an exif block
class ExifIfd : public TiffIfd {
 public:
 ExifIfd(FILE *pFile, unsigned int ifdoffset, bool loadall = false,
	 unsigned int subfileoffset=0):
  TiffIfd(pFile, ifdoffset, loadall, subfileoffset) {
  }
};
}  // namespace jpeg_redaction
#endif // JPEG_REDACTION_LIB_TIFF_IFD
