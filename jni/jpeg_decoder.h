#ifndef INCLUDE_JPEGDECODER
#define INCLUDE_JPEGDECODER
#include <vector>
#include <string>
#include <stdio.h>
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
	      int length,
	      const std::vector<JpegDHT *> &dhts,
	      const std::vector<Jpeg::JpegComponent*> *components);


  // Decode the whole image.
  void Decode() {
    // Reserve space for the redacted data- should be smaller than the original.
    //    redacting_ = (redact ? 1 : 0);

    if (redaction_.NumRegions() > 0)
      redacting_ = 1;

    if (redacting_) {
      redacted_data_.reserve(length_ + 2); // For end marker later.
    }
    redaction_bit_pointer_ = 0;
    redacted_data_.clear();
    while (1) {
      if (redacting_) {
	// Determine if we're in or on the edge of.
	bool in_rr = InRedactionRegion();
	if (in_rr) {
	  if (redacting_ == 1)
	    redacting_ = 3;  // Start.
	  else
	    redacting_ = 2;  // Steady state redacting.
	} else {
	  if (redacting_ == 1 || redacting_ == 5)
	    redacting_ = 1;  // Steady state not redacting.
	  else
	    redacting_ = 5;  // Stop.
	}
      }
      //      if (mcus_ >= 246) debug = 3;
      // Find the matching symbol.
      DecodeOneMCU();
      ++mcus_;
      if (mcus_ >= num_mcus_) {
	printf("Got to %d mcus. %d bits left.\n", num_mcus_,
	       length_ * 8 - data_pointer_);
	break;
      }
    }
  }
  // Fill current_bits up to a complete word.
  void FillBits() {
    int byte = data_pointer_ >> 3;
    // The remaining bits in this byte.
    int new_bits = 8 - (data_pointer_ & 0x7);
    data_pointer_ += word_size_ - num_bits_;
    if (data_pointer_ > (length_ << 3))
      data_pointer_ = length_ << 3;
    while (num_bits_ < word_size_ && byte < length_) {
      /* if (new_bits == 8 && data_[byte] == 0xff && data_[byte+1] != 0x00) { */
      /* 	throw("Don't yet handle internal tags"); */
      /* } */
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
      // Skip a stuff byte - we should have recognized a marker already.
      // TODO(aws)
      if (data_[byte] == 0xff && data_[byte+1] == 0x00) {
	if (debug >= 2) printf("Stuff byte\n");
	data_pointer_ += 8;
	++byte;
      }
      ++byte;
      new_bits = 8;
    }
  }
  void WriteImageData(const char *const filename) {
    FILE *pFile = fopen(filename, "wb");
    fprintf(pFile, "P5\n%d %d %d\n", w_blocks_, h_blocks_, 255);
    printf("Saving Image %d, %d = %d pixels. %lu bytes\n",
	   w_blocks_, h_blocks_, w_blocks_*h_blocks_, image_data_.size());
    fwrite(&image_data_[0], sizeof(unsigned char), image_data_.size(), pFile);
    fclose(pFile);
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
  // Drop the most recent bits from the buffer.
  void DropBits(int len) {
    if (num_bits_ < len)
      throw("Dropping more bits than we have\n");
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
      if (debug > 0) {
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
      if (space <= len && redacted_data_[byte] == 0xff) { // A stuff byte
	redaction_bit_pointer_ += 8;
	redacted_data_.resize((redaction_bit_pointer_ + 7) / 8, 0);
	byte++;
	redacted_data_[byte] == 0x00;
      }
      byte++;
      len -= space;
      bits <<= space;
      space = 8;
    }
    if (debug > 2) printf("Pointer is %d\n", redaction_bit_pointer_);
    if (debug > 0) {
      std::string scurr = Binary(*(unsigned int*)&redacted_data_[0], 32);
      printf("redacted after: %s\n", scurr.c_str());
    }
  }
  int NextValue(int len);
  int DecodeOneBlock(int dht, int comp, int subblock_redaction);

  void WriteValue(int which_dht, int value);
  void WriteZeroLength(int which_dht);
  void ClearRedactionRegions() {
    redaction_.Clear();
  }
  int AddRedactionRegions(const Redaction &redaction) {
    redaction_.Add(redaction);
  }

  // Decode all of the blocks from all of the components in a single MCU.
  void DecodeOneMCU() {
    for (int comp = 0; comp < (*components_).size(); ++comp) {
      int vf = (*components_)[comp]->v_factor_;
      int hf = (*components_)[comp]->h_factor_;
      int dht = (*components_)[comp]->table_;
      if (debug >= 1)
	printf("Decoding MCU %d,%d DHT: %d %dx%d\n", mcus_, dht, comp, hf, vf);
      for (int v = 0; v < vf; ++v) {
	for (int h = 0; h < hf; ++h) {
	  const int subblock = v + h * vf;
	  // redaction encodes the transitions, which are different on the
	  // block level from the MCU.
	  int subblock_redaction = redacting_;
	  if (redacting_ == 3 && subblock != 0)
	    subblock_redaction = 2;
	  if (redacting_ == 5 && subblock != 0)
	    subblock_redaction = 1;
	  int dc_value = 0;
	  try {
	    dc_value = DecodeOneBlock(dht, comp, subblock_redaction);
	  } catch (const char *error) {
	    printf("DecodeOneMCU Caught %s at MCU %d of %d\n",
		   error, mcus_, num_mcus_);
	    throw(error);
	  }
	  if ((*components_)[comp]->table_ == 0) { // Y component
	    if (debug > 0)
	      printf("DCY: %d\n", dc_value);
	    y_value_ += dc_value;
	    while (y_value_ < (-128 << dct_gain_) ||
		   y_value_ >= (128 << dct_gain_)) {
	      ++dct_gain_;
	      printf("MCU %d dc_value %d y_value_ %d. Doubling gain to %d\n",
	    	     mcus_, dc_value, y_value_, dct_gain_);
	      //	      throw("y value out of bounds");
	      for (int op = 0; op < image_data_.size(); ++op)
		image_data_[op]= image_data_[op]/2 + 64;
	    }
	    image_data_.push_back((y_value_ + (128 << dct_gain_)) >> dct_gain_);
	  }
	}
      }
    }
  }

  void ResetDecoding() {
    dct_gain_ = 0; // Number of bits to shift.
    y_value_ = 0;
    current_bits_ = 0;
    num_bits_ = 0;
    data_pointer_ = 0;  // Next bit to get into current_bits;
    //  symbols_ = 0;
    mcus_ = 0;
    mcu_h_ = 1;
    mcu_v_ = 1;
  }

  // Does the current MCU overlap a redaction region.
  bool InRedactionRegion() {
    int mcu_width = w_blocks_ / mcu_h_;
    const int hq = 8 * mcu_h_;
    const int vq = 8 * mcu_v_;
    int mcu_x = mcus_ % mcu_width;
    int mcu_y = mcus_ / mcu_width;
    return redaction_.InRegion(mcu_x * hq, mcu_y * vq, hq, vq);
  }
  // Decoding information:
  unsigned char *data_;
  int length_; // How many bytes in data

  unsigned int current_bits_;
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
  int redacting_; // Are we redacting this image (1 or 2) this MCU (2)?
  int y_value_; // The most recent decoded brightness value.

  int redaction_dc_[3];
  std::vector<unsigned char> image_data_;
  // These are pointers into the Jpeg's table of DHTs
  // The decoder does not own the memory.
  std::vector<JpegDHT *> dhts_;
  const std::vector<Jpeg::JpegComponent*> *components_;

  int redaction_bit_pointer_;
  // Rectangles in original image coords of where we are redacting.
  Redaction redaction_;
  std::vector<unsigned char> redacted_data_;
};
}  // namespace jpeg_redaction

#endif // INCLUDE_JPEGDECODER
