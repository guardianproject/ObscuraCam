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


// TiffIIfd.cpp: implementation of the TiffIfd class.
// (c) 2011 Andrew Senior andrewsenior@google.com

#include "debug_flag.h"
#include "tiff_ifd.h"
#include "tiff_tag.h"
#include "jpeg.h"
#include "byte_swapping.h"

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////


namespace jpeg_redaction {
TiffIfd::TiffIfd(FILE *pFile, unsigned int ifdoffset,
		     bool loadall, unsigned int subfileoffset,
		     bool byte_swapping) :
  subfileoffset_(subfileoffset), byte_swapping_(byte_swapping) {

  if (pFile == NULL)
    return;
  int iRV= fseek(pFile, ifdoffset, SEEK_SET);
  if (iRV != 0) {
    printf("Failed seek");
    fclose(pFile);
    return;
  }
  short nr;
  iRV = fread(&nr, sizeof(short), 1, pFile);
  if (iRV != 1) return;
  if (byte_swapping) ByteSwapInPlace(&nr, 1);
  printf("IFDSize: %d \n", nr);
  for(int tagindex=0 ; tagindex < nr; ++tagindex) {
    int pos = ftell(pFile);
    //    printf("File pointer %d\n", pos);
    TiffTag *tag = new TiffTag(pFile, byte_swapping_);
    tags_.push_back(tag);
  }
  printf("\n DIDIFD\n");
  iRV = fread(&nextifdoffset_, sizeof(unsigned int), 1, pFile);
  if (iRV != 1) {
    throw("Couldn't read nextifdoffset");
  }
  if (byte_swapping) ByteSwapInPlace(&nextifdoffset_, 1);

  // This disrupts the file pointer.
  LoadImageData(pFile, loadall);
  printf("Loaded Image data\n");
  if (loadall)
    LoadAll(pFile);
  Print();
}

  // Print all tags to stdout.
void TiffIfd::Print() const {
  if (debug >= 0)
    printf("%s:%d Listing %zu tags\n", __FILE__, __LINE__, GetNTags());
  TiffTag *width_tag = FindTag(TiffTag::tag_ImageWidth);
  TiffTag *height_tag = FindTag(TiffTag::tag_ImageHeight);
  if (width_tag && height_tag) {
    printf("Image: %dx%d ",
	   width_tag->GetUIntValue(0), height_tag->GetUIntValue(0));
    TiffTag *compression_tag = FindTag(TiffTag::tag_Compression);
    if(compression_tag)
      printf("(%d) ", compression_tag->GetUIntValue(0));
  }

  for(int tagindex=0 ; tagindex < tags_.size(); ++tagindex) {
    tags_[tagindex]->Print();
  printf("\n");
  }
  printf("\n");
}


// Save the ifd out to an open file. Return this ifdlocation
unsigned int TiffIfd::Write(FILE *pFile,
			    unsigned int nextifdoffset,
			    int subfileoffset) const
{
  // Values that need to be filled in later.
  // Record where in the file and then what value will be put there.
  std::vector<unsigned int> pending_pointers_where;
  std::vector<unsigned int> pending_pointers_what;

  int iRV = fseek(pFile, 0, SEEK_END);
  if (iRV != 0 )
    fprintf(stderr, "Failed to fseek in TiffIfd::Write\n");

  // Write the ifd block out.
  const unsigned int ifdstart = ftell(pFile);
  // Write out the IFD with dummies for pointers, and pointer to the
  // image data offset
  short nr = tags_.size();
  iRV = fwrite(&nr, sizeof(short), 1, pFile);
  int length_where = -1;
  for(int tagindex=0 ; tagindex < tags_.size(); ++tagindex) {
    unsigned int pointer = tags_[tagindex]->Write(pFile);   // 0 if no pointer.
    if (pointer) {
      if (tags_[tagindex]->GetTag() == TiffTag::tag_StripByteCounts ||
	  tags_[tagindex]->GetTag() == TiffTag::tag_ThumbnailLength) {
	length_where = pointer;
      } else {
	pending_pointers_where.push_back(pointer);
      }
    }
  }
  iRV = fwrite(&nextifdoffset, sizeof(unsigned int), 1, pFile);

  unsigned int data_length = 0;
  // Write the image or thumbnail data.

  // Write all the subsidiary data that doesn't fit in tags
  // as well as thumbnail/image data.
  for(int tagindex=0 ; tagindex < tags_.size(); ++tagindex) {
    if (tags_[tagindex]->GetTag() == TiffTag::tag_StripOffsets ||
	tags_[tagindex]->GetTag() == TiffTag::tag_ThumbnailOffset) {
      if (data_.empty()) throw("No data to write");
      unsigned int data_start = ftell(pFile);
      // Write the image data out.
      printf("Saving thumbnail to file\n");
      if (jpeg_ == NULL) throw("No JPEG to write out");
      jpeg_->Save(pFile);
      // iRV = fwrite(&data_.front(), sizeof(unsigned char), data_.size(),
      // 		   pFile);
      printf("thumbnail written\n");
      data_length = ftell(pFile) - data_start;
      pending_pointers_what.push_back(data_start);
      continue;
    }

    // Returns 0 if the data is short enough to be in the tag and not
    // in a data block.
    unsigned int pointer =
      tags_[tagindex]->WriteDataBlock(pFile, subfileoffset);
    // "What": where we wrote the datablock.
    if (pointer)
      pending_pointers_what.push_back(pointer);
  }

  // If we're keeping track of pending pointers, fill them in now.
  if (pending_pointers_where.size() != pending_pointers_what.size()) {
    printf("Pending pointers where %zu what %zu\n", 
	   pending_pointers_where.size(), pending_pointers_what.size());
    throw("pending pointers don't match");
  }
  if (length_where >= 0) {
    if (data_length == 0)
      throw("Data length not set");
    fseek(pFile, length_where, SEEK_SET); // Where
    iRV = fwrite(&data_length, sizeof(unsigned int), 1, pFile);
  }
  for(int i = 0; i < pending_pointers_what.size(); ++i) {
    printf("Write TiffIfd Locs Where: %d What: %d-%d\n",
	   pending_pointers_where[i], pending_pointers_what[i], subfileoffset);
    fseek(pFile, pending_pointers_where[i], SEEK_SET); // Where
    const unsigned int ifdloc = pending_pointers_what[i]-subfileoffset;  // What
    iRV = fwrite(&ifdloc, sizeof(unsigned int), 1, pFile);
  }
  // Go back to the end of the file.
  iRV = fseek(pFile, 0, SEEK_END);
  // Return the location of this ifdstart (from which we can calculate
  // where we have to write the nextifdoffset if we hadn't precalculated it)
  return ifdstart;
}

int TiffIfd::LoadAll(FILE *pFile) {
  for(int tagindex=0; tagindex<tags_.size(); ++tagindex) {
    tags_[tagindex]->Load(pFile, subfileoffset_, byte_swapping_);
  }
  return 1;
}

void TiffIfd::Reset() {
  for(int tagindex=0 ; tagindex < tags_.size(); ++tagindex) {
    delete tags_[tagindex];
  }
  tags_.clear();
}


int TiffIfd::LoadImageData(FILE *pFile, bool loadall)
{
  TiffTag *tagdataoffs = FindTag(TiffTag::tag_StripOffsets);
  TiffTag *tagdatabytes = FindTag(TiffTag::tag_StripByteCounts);
  if (tagdataoffs == NULL) {
    if (tagdatabytes !=NULL) throw("offs but no length in data");
    tagdataoffs = FindTag(TiffTag::tag_ThumbnailOffset);
    tagdatabytes = FindTag(TiffTag::tag_ThumbnailLength);
    if (tagdataoffs) {
      if (tagdatabytes == NULL) throw("thumbnail offs but no length in data");
      printf("Loading thumbnail\n");
    }
  }
  data_.clear();
  if (tagdataoffs && tagdatabytes) {
    const unsigned int dataoffs = tagdataoffs->GetUIntValue(0);
    const unsigned int databytes = tagdatabytes->GetUIntValue(0);
    printf("Loading internal image data at %d,%d\n", dataoffs, databytes);
    int iRV = fseek(pFile, dataoffs + subfileoffset_, SEEK_SET);
    if (iRV != 0)
      throw("Can't seek to data");
    jpeg_ = new Jpeg();
    jpeg_->LoadFromFile(pFile, true, dataoffs + subfileoffset_);
    if (loadall) {
      printf("TiffIfd:LoadImageData all\n");
      iRV = fseek(pFile, dataoffs + subfileoffset_, SEEK_SET);
      data_.resize(databytes);
      iRV = fread(&data_.front(), sizeof(unsigned char), databytes, pFile);
      if (iRV != databytes)
        throw("Couldn't read ImageData");
    }

    return databytes;
  }
  return 0;
}

// Add the tag- return 0 if it replaced another tag.
int TiffIfd::AddTag(TiffTag *tag, bool allowmultiple) {
  int tagno = tag->GetTag();
  if (!allowmultiple)
    for(int tagindex=0 ; tagindex < tags_.size(); ++tagindex) {
    if (tags_[tagindex]->GetTag() == tagno) {
      delete tags_[tagindex];
      tags_[tagindex] = tag;
      return 0;
    }
  }

  for(int tagindex=0 ; tagindex < tags_.size(); ++tagindex) {
    if (tags_[tagindex]->GetTag() > tagno) {
      tags_.insert(tags_.begin() + tagindex, tag);
      return 1;
    }
  }
  tags_.push_back(tag);
  return 1;
}
}; // namespace redaction
