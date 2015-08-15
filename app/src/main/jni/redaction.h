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


#ifndef INCLUDE_REDACTION
#define INCLUDE_REDACTION

#include <string>
#include "debug_flag.h"
#include "bit_shifts.h"

namespace jpeg_redaction {
// Class to store information redacted from a horizontal strip of image.
class JpegStrip {
public:
  // Create a strip.
  JpegStrip(int x, int y, int src, int dest) : x_(x), y_(y), src_start_(src),
					       dest_start_(dest) {
    blocks_ = 0;
    bits_ = 0;
  }
  JpegStrip(const std::vector<unsigned char> &pack) {
    Unpack(pack);
  }
  // After finishing a strip, make a copy of the block of data that was removed.
  void SetSrcEnd(int data_end, int blocks) {
    bits_ = data_end - src_start_;
    blocks_ = blocks;
  }
  // After finishing a strip, make a copy of the block of data that was removed.
  void SetDestEnd(const unsigned char* data,
		  int dest_end) {
    const int bytes = (bits_ + 7)/8;
    replaced_by_bits_ = dest_end - dest_start_;
    data_.resize(bytes);
    //    printf("Copying %d bytes from %d\n", bytes, src_start_);
    // copy over bits.
    const int byteoffs = src_start_ / 8;
    const int bitoffs = src_start_ % 8;
    if (bitoffs == 0) {
      memcpy(&data_[0], data + byteoffs, bytes);
    } else {
      // Shift the data down by bitoffs bits while copying it.
      for (int i = 0; i < bytes; ++i) {
	data_[i] = ((data[byteoffs + i] << bitoffs)  & 0xff) +
	  (data[byteoffs + i + 1] >> (8 - bitoffs));
      }
    }
  }
  // Patch this strip into a redacted image with a given bit offset.
  // 0 offset assumes that this is the first strip, or that all previous
  // strips have been inserted.
  // Returns the offset.
  int PatchIn(int offset, std::vector<unsigned char> *data,
	       int *data_bits) const {
    // How much we have to shift the trailing region up.
    const int tail_shift =  bits_ - replaced_by_bits_;
    if (debug > 0)
      printf("Patch at %d (%d) %d->%d\n",
	     src_start_ + offset, offset, replaced_by_bits_, bits_);
    BitShifts::ShiftTail(data, data_bits, src_start_ + offset, tail_shift);
    BitShifts::Overwrite(data, *data_bits,
    			 src_start_ + offset, 
    			 data_, bits_);
    BitShifts::PadLastByte(data, *data_bits);
    return tail_shift;
  }
  bool Valid(int *offset) const {
    if (bits_ < 0) return false;
    if (data_.size() * 8 < bits_) return false;
    if (debug > 0)
      printf("Strip (%d,%d: %d MCUs) has %d bits (rep %d), src %d dest %d "
	     "diff %d offset %d.\n", x_, y_, blocks_,
	     bits_, replaced_by_bits_,
	     src_start_, dest_start_, src_start_ - dest_start_,
	     *offset);
    *offset += bits_ - replaced_by_bits_;
    return true;
  }
  int AppendBlock(const unsigned char* data, int bits) {
    data_.resize((bits_ + bits + 7)/8);
    // copy over bits.
    throw(0);
    bits_ += bits;
    return ++blocks_;
  }
  // Copy an int from one location to another.
  // Changing the byte order if not <TODO>.
  // Also used by Region.
  static void MemcpyByteSwapping(int *dest, const int *src) {
    // TODO(byteswapping) to standard ordering.
    memcpy(dest, src, sizeof(int));
  }
  // Return a blob that contains all the data and can be unpacked into the
  // same structure with Unpack().
  void Pack(std::vector<unsigned char> *pack) const {
    int version = 1;
    if ((bits_ + 7) / 8 != data_.size()) {
      fprintf(stderr, "bits_ %d bytes %d data_.size() %zu\n",
	      bits_, (bits_ + 7) / 8, data_.size());
      throw("size mismatch in pack");
    }
    const int size = GetPackSize();
    //    printf("Packing strip size %d 32+%zu\n", size, data_.size());
    pack->resize(size);
    // TODO(byteswapping) to standard ordering.
    int * store = (int *)&(*pack)[0];
    MemcpyByteSwapping(store++, &version);
    MemcpyByteSwapping(store++, &bits_);
    MemcpyByteSwapping(store++, &src_start_);
    MemcpyByteSwapping(store++, &dest_start_);
    MemcpyByteSwapping(store++, &replaced_by_bits_);
    MemcpyByteSwapping(store++, &x_);
    MemcpyByteSwapping(store++, &y_);
    MemcpyByteSwapping(store++, &blocks_);
    //    printf("Store diff %d\n", ((unsigned char *)store) - &pack[0]);
    memcpy(store, &data_[0], data_.size() * sizeof(unsigned char));
  }
  // Take a blob of data created by Pack and insert it into the Strip object.
  void Unpack(std::vector<unsigned char> const &pack) {
    int version = 0;
    const int * store = (const int *)&pack[0];
    MemcpyByteSwapping(&version, store++);
    if (version != 1) throw("Wrong version in redaction.h: Unpack");
    MemcpyByteSwapping(&bits_, store++);
    const int data_size = pack.size() - sizeof(int) * 8;
    if ((bits_ + 7) / 8 != data_size)
      throw("Data size mismatch in unpack");
    data_.resize(data_size);
    MemcpyByteSwapping(&src_start_, store++);
    MemcpyByteSwapping(&dest_start_, store++);
    MemcpyByteSwapping(&replaced_by_bits_, store++);
    MemcpyByteSwapping(&x_, store++);
    MemcpyByteSwapping(&y_, store++);
    MemcpyByteSwapping(&blocks_, store++);
    memcpy(&data_[0], store, data_size * sizeof(unsigned char));
  }    
  // How many bytes are needed to store this object's Pack().
  int GetPackSize() const {
    return sizeof(int) * 8 + data_.size();
  }
protected:
  std::vector<unsigned char> data_; // Raw binary encoded data.
  int bits_;
  // In the original strip at what bit did the strip start.
  int src_start_;
  // In the redacted version at what bit does the start correspond to.
  int dest_start_;
  // In the redacted version how many bits was this strip replaced by.
  int replaced_by_bits_;
  int x_; // Coordinate of start (from left)
  int y_; // Coordinate of start (from top)
  int blocks_; // Number of blocks stored.
};

// Class to define the areas to be redacted, and return the strips of 
// redacted information.
class Redaction {
public:
  enum redaction_method {redact_copystrip = 0,
			 redact_solid = 1,
			 redact_pixellate = 2,
			 redact_overlay   = 3,
			 redact_inverse_pixellate = 4};

  // Simple rectangle class for redaction regions.
  class Region {
  public:
    // Top left is 0,0 so t<b, l<r.
    Region(int l, int r, int t, int b) : l_(l), r_(r), t_(t), b_(b) {
      redaction_method_ = redact_solid;
      if (l >= r || t >= b) throw("Bad rectangle created");
    }
    Region()  : l_(0), r_(0), t_(0), b_(0) {}
    void SetRedactionMethod(redaction_method method) {
      redaction_method_ = method;
    }
    void SetRedactionMethod(const char *const method) {
      switch (method[0]) {
      case 'c':
	redaction_method_ = redact_copystrip;
	break;
      case 's':
	redaction_method_ = redact_solid;
	break;
      case 'p':
	redaction_method_ = redact_pixellate;
	break;
      case 'o':
	redaction_method_ = redact_overlay;
	break;
      case 'i':
	redaction_method_ = redact_inverse_pixellate;
	break;
      default:
	fprintf(stderr, "Unknown redaction method: %s\n", method);
	return;
      }
    }
    redaction_method GetRedactionMethod() const {
      return redaction_method_;
    }
    // Return the larger of width and height.
    int GetWidth() const {
      return r_ - l_;
    }
    int GetHeight() const {
      return b_ - t_;
    }
    void Pack(std::vector<unsigned char> *pack) const {
      pack->resize(5 * sizeof(int));
      int * pack_ptr = (int*)&(*pack)[0];
      JpegStrip::MemcpyByteSwapping(pack_ptr++, &l_);
      JpegStrip::MemcpyByteSwapping(pack_ptr++, &r_);
      JpegStrip::MemcpyByteSwapping(pack_ptr++, &t_);
      JpegStrip::MemcpyByteSwapping(pack_ptr++, &b_);
      JpegStrip::MemcpyByteSwapping(pack_ptr++,
				    (const int *)&redaction_method_);
    }
    void Unpack(std::vector<unsigned char> const &pack) {
      if (pack.size() != 5 * sizeof(int))
	throw("Region pack is not 5 * sizeof(int)");
      int * pack_ptr = (int*)&pack[0];
      JpegStrip::MemcpyByteSwapping(&l_, pack_ptr++);
      JpegStrip::MemcpyByteSwapping(&r_, pack_ptr++);
      JpegStrip::MemcpyByteSwapping(&t_, pack_ptr++);
      JpegStrip::MemcpyByteSwapping(&b_, pack_ptr++);
      JpegStrip::MemcpyByteSwapping((int*)&redaction_method_, pack_ptr++);
    }
    int l_, r_, t_, b_;

  protected:
    redaction_method redaction_method_;
  };
  Redaction() {}
  virtual ~Redaction() {
    for (int i = 0; i < strips_.size(); ++i)
      delete strips_[i];
  }
  Redaction *Copy() {
    Redaction *copy = new Redaction;
    for (int i = 0; i < regions_.size(); ++i) {
      if (debug > 0)
	printf("adding region %d of %zu\n", i, regions_.size());
      copy->AddRegion(regions_[i]);
    }
    return copy;
  }
  void AddRegion(const Region &rect) {
    if (rect.l_ >= rect.r_ || rect.t_ >= rect.b_) {
      fprintf(stderr, "Bad region %d %d %d %d\n",
	     rect.l_, rect.r_, rect.t_, rect.b_);
      throw("region badly formed l>=r or t>=b");
    }
    regions_.push_back(rect);
  }

  // Make a region from a string of comma coordinates: l,r,t,b:method
  void AddRegion(const std::string &rect_string) {
    int l, r, t, b;
    if (rect_string.empty())
      return;
    char method[11];
    int rv = sscanf(rect_string.c_str(), "%d,%d,%d,%d:%10s", &l, &r, &t, &b,
		    method);
    if (rv != 4 && rv != 5) {
      std::string message("Region string badly formed, should be l,r,t,b");
      message += rect_string;
      throw(message.c_str());
    }
    Region rect(l, r, t, b);
    if (rv == 5)
      rect.SetRedactionMethod(method);
    regions_.push_back(rect);
  }
  // Make regions from semi-colon-separated regions l,r,t,b[:method];l,r,t,b...
  void AddRegions(const std::string &rect_strings) {
    int start = 0;
    do { 
      int end = rect_strings.find(';', start);
      if (end == rect_strings.npos)
	end = rect_strings.length();
      std::string sub = rect_strings.substr(start, end - start);
      if (!sub.empty()) {
	if (sub.length() < 7)
	  throw("Can't parse rect_strings properly");
	AddRegion(sub);
      }
      start = end + 1;
    } while (start < rect_strings.length());
  }
  // Check that all the strips are consistent.
  bool ValidateStrips() {
    int offset = 0;
    if (debug > 0)
      printf("Redaction::%zu strips\n", strips_.size());
    for (int i = 0; i < strips_.size(); ++i) {
      if (!strips_[i]->Valid(&offset))
	return false;
    }
    return true;
  }
  // Pack up the regions and the strips into a single blob that can 
  // be unpacked later.
  void Pack(std::vector<unsigned char> *pack) {
    if (!ValidateStrips())
      throw("Couldn't validate strips before packing");
    const int num_strips = NumStrips();
    const int num_regions = NumRegions();
    // Calculate how many bytes are needed for the whole pack.
    // Store (as ints) the number of strips and regions,
    // the length of each strip's blob, then l,r,t,b,type for each region
    // and then an unsiged char blob for each strip.
    int size = sizeof(int) * (2 + NumStrips() + 5  * NumRegions());
    for (int i = 0; i < NumStrips(); ++i) {
      const int strip_size = strips_[i]->GetPackSize();
      size += strip_size;
    }
    if (debug > 1)
      printf("Packing redaction size %d, %d strips, %d regions\n", size,
	     NumStrips(), NumRegions());
    // Now start assembling the pack:
    pack->resize(size);
    unsigned char *packptr = &(*pack)[0];
    JpegStrip::MemcpyByteSwapping((int *)packptr, &num_strips);
    packptr += sizeof(int);
    JpegStrip::MemcpyByteSwapping((int *)packptr, &num_regions);
    packptr += sizeof(int);
    for (int i = 0; i < NumRegions(); ++i) {
      std::vector<unsigned char> packed_region;
      regions_[i].Pack(&packed_region);
      if (packed_region.size() != 5 * sizeof(int))
	throw("Region size is not 5 unsigned ints");
      memcpy(packptr, &packed_region[0], packed_region.size());
      packptr += packed_region.size();
    }
    for (int i = 0; i < NumStrips(); ++i) {
      const int strip_size = strips_[i]->GetPackSize();
      JpegStrip::MemcpyByteSwapping((int *)packptr, &strip_size);
      packptr += sizeof(int);
      std::vector<unsigned char> packed_strip;
      strips_[i]->Pack(&packed_strip);
      if (packed_strip.size() != strip_size) {
	printf("Strip size mismatch %zu vs %d\n",
	       packed_strip.size(), strip_size);
	throw("strip's actual size is not the same as predicted");
      }
      memcpy(packptr, &packed_strip[0], packed_strip.size());
      packptr += packed_strip.size();
    }
    if (packptr != &(*pack)[0] + size) {
      throw("After packing pointer is not start + size");
    }
  }

  // Take a binary pack and turn it into a redaction object.
  void Unpack(const std::vector<unsigned char> &pack) {
    regions_.clear();
    strips_.clear();
    int num_strips = 0;
    int num_regions = 0;
    const unsigned char *packptr = &pack[0];
    JpegStrip::MemcpyByteSwapping(&num_strips, (const int *)packptr);
    packptr += sizeof(int);
    JpegStrip::MemcpyByteSwapping(&num_regions, (const int *)packptr);
    packptr += sizeof(int);

    int pack_size = sizeof(int) * (2 + num_strips + 5  * num_regions);
    regions_.resize(num_regions);
    if (debug > 2)
      printf("Unpacking: %d regions, %d strips. %zu bytes\n",
	     num_regions, num_strips, pack.size());
    for (int i = 0; i < num_regions; ++i) {
      std::vector<unsigned char> packed_region;
      const unsigned int size = 5 * sizeof(int);
      packed_region.resize(size);
      memcpy(&packed_region[0], packptr, size);
      regions_[i].Unpack(packed_region);
      packptr += size;
    }
    for (int i = 0; i < num_strips; ++i) {
      std::vector<unsigned char> packed_strip;
      int strip_size;
      JpegStrip::MemcpyByteSwapping(&strip_size, (int *)packptr);
      packptr += sizeof(int);
      packed_strip.resize(strip_size);
      memcpy(&packed_strip[0], packptr, strip_size);
      JpegStrip *one_strip = new JpegStrip(packed_strip);
      strips_.push_back(one_strip);
      packptr += strip_size;
      pack_size += strip_size;
    }
    if (packptr != &pack[0] + pack_size ||
	pack_size != pack.size()) {
      fprintf(stderr, "pack.size() %zu. Calculated %d ptrdiff %ld\n",
	      pack.size(), pack_size, packptr-&pack[0]);
      throw("After packing pointer is not start + size");
    }
  }
  void Add(const Redaction &red) {
    for (int i = 0; i < red.NumRegions(); ++i)
      regions_.push_back(red.GetRegion(i));
  }
  Region GetRegion(int i) const {
    return regions_[i];
  }
  int NumRegions() const {
    return regions_.size();
  }
  void Clear() {
    regions_.clear();
  }
  int NumStrips() const {
    return strips_.size();
  }
  const JpegStrip* GetStrip(int strip_index) const {
    return strips_[strip_index];
  }
  void AddStrip(const JpegStrip *strip) {
    strips_.push_back(strip);
  }
  void Scale(int new_width, int new_height,
	     int old_width, int old_height) {
    if (debug > 0)
      printf("Scaling %dx%d -> %dx%d\n",
	     old_width, old_height,
	     new_width, new_height);
    for (int i = 0; i< regions_.size(); ++i) {
      regions_[i].l_ = (regions_[i].l_ * new_width) / old_width;
      regions_[i].r_ = (regions_[i].r_ * new_width) / old_width;
      regions_[i].t_ = (regions_[i].t_ * new_height) / old_height;
      regions_[i].b_ = (regions_[i].b_ * new_height) / old_height;
    }
  }
  // Test if a box of width dx, dy, with top left corner at x,y
  // intersects with any of the rectangular regions.
  // if so return the index of the region. If not return -1;
  int InRegion(int x, int y, int dx, int dy) const {
    bool inverting = false;
    int region = -1;
    for (int i = 0; i < regions_.size(); ++i) {
      // If this is the first "inverse" region, record that previously
      // we WERE in the redaction region.
      // If we were in any other kind of region, still apply that method.
      if (regions_[i].GetRedactionMethod() == redact_inverse_pixellate) {
	if (!inverting && region < 0)
	  region = i;
	inverting = true;
      }
      if (x      < regions_[i].r_ &&
          x + dx > regions_[i].l_ &&
          y      < regions_[i].b_ &&
          y + dy > regions_[i].t_) {
	if (regions_[i].GetRedactionMethod() == redact_inverse_pixellate) {
	  region = -1;
	} else {
	  region = i;
	}
      }
      }
    return region;
  }
protected:
  // Information redacted.
  std::vector<const JpegStrip*> strips_;
  std::vector<Region> regions_;
};
} // namespace jpeg_redaction
#endif // INCLUDE_REDACTION
