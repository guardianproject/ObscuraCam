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


#ifndef INCLUDE_JPEGDECODER
#define INCLUDE_JPEGDECODER
// JpegDecoder parse the JPEG encoded data.
// 2011 Andrew Senior
#include <vector>
#include <string>
#include <stdio.h>
#include "bit_shifts.h"
#include "jpeg.h"
#include "jpeg_dht.h"
#include "redaction.h"

extern int debug;
namespace jpeg_redaction {
class JpegDHT;
class Jpeg;
class JpegDecoder {
 public:
  JpegDecoder(int w, int h,
	      unsigned char *data,
	      int length,  // in bits of the data.
	      const std::vector<JpegDHT *> &dhts,
	      const std::vector<Jpeg::JpegComponent*> *components);


  // Decode the whole image.
  void Decode(Redaction *redaction);

  const std::vector<unsigned char> &GetRedactedData() {
    if (redaction_bit_pointer_ > (redacted_data_.size() * 8) || 
	redaction_bit_pointer_ < (redacted_data_.size() * 8) - 8) {
      throw("RedactedData length mismatch");
    }
    BitShifts::PadLastByte(&redacted_data_, redaction_bit_pointer_);
    return redacted_data_;
  }
  // Write the grey scale decoded image to a file.
  // Return 0 on success.
  int WriteImageData(const char *const filename) {
    FILE *pFile = fopen(filename, "wb");
    if (pFile == NULL)
      return 1;
    int rv = fprintf(pFile, "P5\n%d %d %d\n", w_blocks_, h_blocks_, 255);
    printf("Saving Image %d, %d = %d pixels. %lu bytes\n",
	   w_blocks_, h_blocks_, w_blocks_*h_blocks_, image_data_.size());
    rv = fwrite(&image_data_[0], sizeof(unsigned char),
		image_data_.size(), pFile);
    fclose(pFile);
    if (rv != image_data_.size())
      return 1;
    return 0;
  }
  // ImageData arrived as zig-zags of blocks within MCU.
  // They were added linearly to image_data_
  // Rearrange them into proper raster order.
  void ReorderImageData() {
    int hf = (*components_)[0]->h_factor_;
    int vf = (*components_)[0]->v_factor_;
    printf("Reordering Image %d, %d = %d pixels. %lu bytes. h,v %d,%d\n",
	   w_blocks_, h_blocks_, w_blocks_ * h_blocks_,
           image_data_.size(), hf, vf);
    int block_size = vf * hf;
    std::vector<unsigned char> c(w_blocks_ * h_blocks_, 0);
    for (int i = 0 ; i < w_blocks_ * h_blocks_; ++i) {
      int block = i / block_size;
      int sub = i % block_size;
      int x = hf * (block % (w_blocks_/hf)) + (sub % hf);
      int y = vf * (block / (w_blocks_/hf)) + (sub / hf);
      int newpos = x + y* w_blocks_;
      if (x > w_blocks_ || y > h_blocks_)
	printf("Reordered Data from %d: %d,%d out of bounds\n",
	       i, x, y);
      c[newpos] = image_data_[i];
    }
    image_data_.assign(c.begin(), c.end());
  }
  // Return the current length of the data block (in bits).
  int GetBitLength() const { return length_; }


protected:
  // Fill current_bits up to a complete word.
  // Data bits are "as written" ie first bit is highest order bit of first 
  // byte. current_bits is a word where the next bit is the highest order
  // bit. We shuffle in data so that there are at least N (16?) bits
  // at all times.
  // Assumes that stuff bytes and tags have already been removed.
  // data_pointer is the next bit to shift into current_bits_
  // num_bits_ is the number of bits in current_bits_
  void FillBits() {
    int byte = data_pointer_ >> 3;
    // The remaining bits in this byte.
    int new_bits = 8 - (data_pointer_ & 0x7);
    data_pointer_ += word_size_ - num_bits_;
    if (data_pointer_ > length_)
      data_pointer_ = length_;
    while (num_bits_ < word_size_ && byte < ((length_ + 7) >> 3)) {
      unsigned int val = (data_[byte] & ((1<<new_bits)-1));
      const int shift = word_size_ - new_bits - num_bits_;
      if (shift < 0) {
	val >>= -shift;
	current_bits_ |= val;
	num_bits_ = word_size_;
	return;
      }
      current_bits_ |= (val << shift);
      num_bits_ += new_bits;
      ++byte;
      new_bits = 8;
    }
  }
  // Drop the most recent bits from the buffer.
  void DropBits(int len) {
    if (num_bits_ < len) {
      printf("Dropping %d left %d\n", len, num_bits_);
      throw("Dropping more bits than we have\n");
    }
    current_bits_ <<= len;
    num_bits_ -= len;
  }

  // Take bits from the original stream and put them in the redacted stream.
  void CopyBits(int len) {
    InsertBits(current_bits_, len);
  }
  // Take the top len bits from the uint & add them to redacted data.
  void InsertBits(unsigned int bits, int len) {
    redacted_data_.resize((len + redaction_bit_pointer_ + 7) / 8, 0);
    /* if (debug > 0) { */
    /*   std::string scurr = Binary(*(unsigned int*)&redacted_data_[0], 32); */
    /*   printf("redacted currently: %s\n", scurr.c_str()); */
    /* } */
    // How many bits are available in the current target byte.
    int space = 8 -(redaction_bit_pointer_ % 8);
    int byte = redaction_bit_pointer_ / 8;
    redaction_bit_pointer_ += len;
    while (len > 0) {
      if (debug > 3) {
	std::string s = Binary(bits >> (32-len), len);
	printf("inserting %d bits: %s\n", len, s.c_str());
      }
      unsigned char newbits = bits >> (32 - space);
      // Clear the trailing bits we're not using.
      if (space > len)
	newbits &= ~ ((1<< (space - len))-1);
      if (byte >= redacted_data_.size())
	throw(0);
      redacted_data_[byte] |= newbits;
      // if (space <= len && redacted_data_[byte] == 0xff) { // A stuff byte
      // 	redaction_bit_pointer_ += 8;
      // 	redacted_data_.resize((redaction_bit_pointer_ + 7) / 8, 0);
      // 	byte++;
      // 	redacted_data_[byte] == 0x00;
      // }
      byte++;
      len -= space;
      bits <<= space;
      space = 8;
    }
    if (debug > 2) printf("Pointer is %d\n", redaction_bit_pointer_);
    if (debug > 3) {
      std::string scurr = Binary(*(unsigned int*)&redacted_data_[0], 32);
      printf("redacted after: %s\n", scurr.c_str());
    }
  }
  int NextValue(int len);
  int DecodeOneBlock(int dht, int comp, int subblock_redaction);
  int LookupPixellationValue(int comp);

  int WriteValue(int which_dht, int value);
  void WriteZeroLength(int which_dht);

  // Update the redacting_ state flag. According to whether we're in
  // a redaction region or on its edge.
  void SetRedactingState(Redaction *redaction);

  // Decode all of the blocks from all of the components in a single MCU.
  void DecodeOneMCU();
  // At the end of a redaction strip, store the redacted bits in Redaction
  // object.
  void StoreEndOfStrip(Redaction *redaction);

  void ResetDecoding() {
    redacting_ = kRedactingOff;
    redacted_data_.clear();
    redaction_bit_pointer_ = 0;

    dct_gain_ = 0; // Number of bits to shift.
    current_bits_ = 0;  // Buffer of 32 bits.
    num_bits_ = 0;  // Number of bits remaining in current_bits_
    data_pointer_ = 0;  // Next bit to get into current_bits;
    mcus_ = 0;
  }

  // Return x & y coord of the top left corner of this MCU.
  int GetX(int mcu) const {
    const int mcu_width = w_blocks_ / mcu_h_;
    const int hq = kBlockSize * mcu_h_;
    const int mcu_x = mcus_ % mcu_width;
    return mcu_x * hq;
  }
  int GetY(int mcu) const {
    const int mcu_width = w_blocks_ / mcu_h_;
    const int vq = kBlockSize * mcu_v_;
    const int mcu_y = mcus_ / mcu_width;
    return mcu_y * vq;
  }
    
  // Does the current MCU overlap a redaction region.
  // return the index of the region, -1 if not
  int InRedactionRegion(const Redaction *const redaction) const {
    const int mcu_width = w_blocks_ / mcu_h_;
    const int hq = kBlockSize * mcu_h_;
    const int vq = kBlockSize * mcu_v_;
    const int mcu_x = mcus_ % mcu_width;
    const int mcu_y = mcus_ / mcu_width;
    return redaction->InRegion(mcu_x * hq, mcu_y * vq, hq, vq);
  }

  // First MCU of redaction region.
  static const int kRedactingStarting;
  // First MCU after redaction region.
  static const int kRedactingEnding;
  // Redacting this point of the image.
  static const int kRedactingActive;
  // Redacting this image but not at this point of the image.
  static const int kRedactingInactive;
  // No redaction requested for this image.
  static const int kRedactingOff;

  // Width/height of a block. (ie 8 pixels)
  static const int kBlockSize;

  // Decoding information:
  unsigned char *data_;
  int length_; // How many bits in data

  unsigned int current_bits_;  // Buffer of up to 32 bits.
  int num_bits_; // How many bits valid in current_bits_

  int data_pointer_;  // Next bit to get into current_bits;
  //  int symbols_;
  int num_mcus_;  // How many we expect.
  int mcus_; // How many we've found
  //bits in JpegDecoder::current_bits_);
  static const int word_size_  = 8 * sizeof(unsigned int);
  int mcu_h_;
  int mcu_v_;
  int width_;
  int height_;
  int w_blocks_;  // Width of the image in MCUs
  int h_blocks_;  // Height of the image in MCUs
  int dct_gain_;
  int redacting_; // Are we redacting this image: see kRedacting* flags above.

  // What is the redaction method for the current region.
  Redaction::redaction_method  redaction_method_;
  // Index of the region that we're currently in.
  int region_index_;
  // The current (input) DC (delta) value for this MCU, one per channel.
  std::vector<int> dc_values_;
  // The current DC (delta) value for this MCU, one per channel, as output.
  // Differs from dc_values_ when we're redacting.
  std::vector<int> redaction_dc_;

  // This stores the cumulative DC values for all components, interleaved.
  // before down-scaling.
  std::vector<int> int_image_data_;
  // The DC values scaled to bytes. Currently intensity only.
  // Initially in MCU order, but then reordered to raster for writing as a pgm.
  std::vector<unsigned char> image_data_;
  // These are pointers into the Jpeg's table of DHTs
  // The decoder does not own the memory.
  std::vector<JpegDHT *> dhts_;
  const std::vector<Jpeg::JpegComponent*> *components_;

  int redaction_bit_pointer_;
  std::vector<unsigned char> redacted_data_;
  JpegStrip *current_strip_;
  // Pointer to the current redaction, while decoding.
  Redaction *redaction_;
};
}  // namespace jpeg_redaction

#endif // INCLUDE_JPEGDECODER
