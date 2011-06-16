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
    printf("Patch at %d (%d) %d->%d\n",
	   src_start_ + offset, offset, replaced_by_bits_, bits_);
    BitShifts::ShiftTail(data, data_bits, src_start_ + offset, tail_shift);
    BitShifts::Overwrite(data, *data_bits,
    			 src_start_ + offset, 
    			 data_, bits_);
    return tail_shift;
  }
  bool Valid(int *offset) const {
    if (bits_ < 0) return false;
    if (data_.size() * 8 < bits_) return false;
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
  // Simple rectangle class for redaction regions.
  class Rect {
   public:
    // Top left is 0,0 so t<b, l<r.
    Rect(int l, int r, int t, int b) : l_(l), r_(r), t_(t), b_(b) {
      if (l >= r || t >= b) throw("Bad rectangle created");
    }
    int l_, r_, t_, b_;
  };
  Redaction() {};
  virtual ~Redaction() {
    for (int i = 0; i < strips_.size(); ++i)
      delete strips_[i];
  }
  void AddRegion(const Rect &rect) {
    if (rect.l_ >= rect.r_ || rect.t_ >= rect.b_)
      throw("region badly formed l>=r or t>=b");
    regions_.push_back(rect);
  }
  void AddRegion(const std::string &rect_string) {
    int l, r, t, b;
    int rv = sscanf("%d,%d,%d,%d", rect_string.c_str());
    if (rv != 4)
      throw("Region string badly formed, should be l,r,t,b");
    Rect rect(l, r, t, b);
    regions_.push_back(rect);
  }
  // Check that all the strips are consistent.
  bool ValidateStrips() {
    int offset = 0;
    printf("Redaction::%d strips\n", strips_.size());
    for (int i = 0; i < strips_.size(); ++i) {
      if (!strips_[i]->Valid(&offset))
	return false;
    }
    return true;
  }
  void Add(const Redaction &red) {
    for (int i = 0; i < red.NumRegions(); ++i)
      regions_.push_back(red.GetRegion(i));
  }
  Rect GetRegion(int i) const {
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
  // Test if a box of width dx, dy, with top left corner at x,y
  // intersects with any of the rectangular regions.
  bool InRegion(int x, int y, int dx, int dy) const {
    for (int i = 0; i < regions_.size(); ++i)
      if (x      < regions_[i].r_ &&
          x + dx > regions_[i].l_ &&
          y      < regions_[i].b_ &&
          y + dy > regions_[i].t_) {
        return true;
      }
    return false;
  }
protected:
  // Information redacted.
  std::vector<const JpegStrip*> strips_;
  std::vector<Rect> regions_;
};
} // namespace jpeg_redaction
#endif // INCLUDE_REDACTION
