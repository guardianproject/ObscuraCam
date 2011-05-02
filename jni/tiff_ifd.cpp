// TiffIIfd.cpp: implementation of the TiffIfd class.
// (c) 2011 Andrew Senior andrewsenior@google.com

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
  data_(NULL), subfileoffset_(subfileoffset), byte_swapping_(byte_swapping) {

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
  printf("Listing tags\n");
  ListTags();
}

void TiffIfd::ListTags() const {
  TiffTag *width_tag = FindTag(TiffTag::tag_width);
  TiffTag *height_tag = FindTag(TiffTag::tag_height);
  if (width_tag && height_tag) {
    printf("Image: %dx%d ", width_tag->GetUIntValue(0), height_tag->GetUIntValue(0));
    TiffTag *compression_tag = FindTag(TiffTag::tag_compression);
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
unsigned int TiffIfd::Write(FILE *pFile, unsigned int nextifdoffset,
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
  // Write out the IFD with dummies for pointers.
  short nr = tags_.size();
  iRV = fwrite(&nr, sizeof(short), 1, pFile);
  int whereisdata = -1;
  for(int tagindex=0 ; tagindex < tags_.size(); ++tagindex) {
    unsigned int pointer = tags_[tagindex]->Write(pFile);   // 0 if no pointer.
    if (pointer) {
      if (tags_[tagindex]->GetTag() == TiffTag::tag_stripoffset ||
	  tags_[tagindex]->GetTag() == TiffTag::tag_thumbnailoffset)
        whereisdata = pending_pointers_where.size();  // Which entry corresponds to the strips/thumbnail
      pending_pointers_where.push_back(pointer); // "Where":  Where we stored the pointer
    }
  }
  iRV = fwrite(&nextifdoffset, sizeof(unsigned int), 1, pFile);

  unsigned int datastart = 0;
  // Write the image or thumbnail data.
  if (data_) {
    TiffTag *tagdatabytes = FindTag(TiffTag::tag_stripbytes);
    if (tagdatabytes == NULL)
      tagdatabytes = FindTag(TiffTag::tag_thumbnaillength);
    datastart = ftell(pFile);
    iRV = fwrite(data_, sizeof(unsigned char), tagdatabytes->GetUIntValue(0),
		 pFile);
  }

  // Write all the subsidiary data.
  for(int tagindex=0 ; tagindex < tags_.size(); ++tagindex) {
    if (datastart && pending_pointers_what.size() == whereisdata)
      pending_pointers_what.push_back(datastart);  // "Where" we wrote the datablock.
    unsigned int pointer = tags_[tagindex]->WriteDataBlock(pFile, subfileoffset);
    if (pointer) pending_pointers_what.push_back(pointer);  // "What": where we wrote the datablock.
  }
  if (datastart && pending_pointers_what.size() == whereisdata)
    pending_pointers_what.push_back(datastart);  // "Where" we wrote the datablock.

  // If we're keeping track of pending pointers, fill them in now.
  if (pending_pointers_where.size() != pending_pointers_what.size())
    throw("pending pointers don't match");
  for(int i = 0; i < pending_pointers_what.size(); ++i) {
    printf("Write TiffIfd Locs Where: %d What: %d-%d\n",
	   pending_pointers_where[i], pending_pointers_what[i], subfileoffset);
    fseek(pFile, pending_pointers_where[i], SEEK_SET); // Where
    const unsigned int ifdloc = pending_pointers_what[i]-subfileoffset;  // What
    iRV = fwrite(&ifdloc, sizeof(unsigned int), 1, pFile);
  }
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
  TiffTag *tagdataoffs = FindTag(TiffTag::tag_stripoffset);
  TiffTag *tagdatabytes = FindTag(TiffTag::tag_stripbytes);
  if (tagdataoffs == NULL) {
    if (tagdatabytes !=NULL) throw("offs but no length in data");
    tagdataoffs = FindTag(TiffTag::tag_thumbnailoffset);
    tagdatabytes = FindTag(TiffTag::tag_thumbnaillength);
  }
  delete [] data_;
  data_ = NULL;
  if (tagdataoffs && tagdatabytes) {
    const unsigned int dataoffs = tagdataoffs->GetUIntValue(0);
    const unsigned int databytes = tagdatabytes->GetUIntValue(0);
    printf("Loading internal image data at %d,%d\n", dataoffs, databytes);
    int iRV = fseek(pFile, dataoffs + subfileoffset_, SEEK_SET);
    if (iRV != 0)
      throw("Can't seek to data");
    jpeg_ = new Jpeg();
    jpeg_->LoadFromFile(pFile, false, dataoffs + subfileoffset_);
    if (loadall) {
      printf("Loading all\n");
      iRV = fseek(pFile, dataoffs + subfileoffset_, SEEK_SET);
      data_ = new unsigned char [databytes];
      iRV = fread(data_, sizeof(unsigned char), databytes, pFile);
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
