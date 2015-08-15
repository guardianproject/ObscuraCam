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


// JpegDecoder class: parse the JPEG encoded data.
#include <stdio.h>
#include "jpeg_decoder.h"
#include "jpeg.h"
#include "jpeg_dht.h"

namespace jpeg_redaction {
const int JpegDecoder::kRedactingStarting = 3;
const int JpegDecoder::kRedactingEnding = 5;
const int JpegDecoder::kRedactingActive = 2;
const int JpegDecoder::kRedactingInactive = 1;
const int JpegDecoder::kRedactingOff = 0;

const int JpegDecoder::kBlockSize = 8;
JpegDecoder::JpegDecoder(int w, int h,
			 unsigned char *data,
			 int length,  // in bits
			 const std::vector<JpegDHT *> &dhts,
			 const std::vector<Jpeg::JpegComponent*> *components) :
  height_(h), width_(w), components_(components), current_strip_(NULL) {
  data_ = data;
  length_ = length;
  mcu_h_ = 1;
  mcu_v_ = 1;
  ResetDecoding();
  // Build a temp table of the DHTs to use for each component
  // and find the size of the MCU.
  for (int comp = 0; comp < components->size(); ++comp) {
    if ((*components)[comp]->h_factor_ > mcu_h_)
      mcu_h_ = (*components)[comp]->h_factor_;
    if ((*components)[comp]->v_factor_ > mcu_v_)
      mcu_v_ = (*components)[comp]->v_factor_;
    JpegDHT *ac_dht = NULL;
    JpegDHT *dc_dht = NULL;
    int i;
    for (i = 0; i < dhts.size() &&
	   (ac_dht == NULL || dc_dht == NULL); ++i) {
      if (dhts[i]->id_ == (*components)[comp]->table_) {
	if (debug > 0)
	  printf("Comp %d %d%s DHT: %d\n",
		 comp, dhts[i]->class_, (dhts[i]->class_? "AC":"DC"), i);
	if (dhts[i]->class_ == 0)
	  dc_dht = dhts[i];
	else
	  ac_dht = dhts[i];
      }
    }
    if (ac_dht == NULL || dc_dht == NULL) {
      for (int i = 0; i < dhts.size(); ++i)
      	fprintf(stderr, "DHT %d %p id %d\n", i, dhts[i], dhts[i]->id_);
      fprintf(stderr, "comp %d of %zu table %d dhts %zu AC %p dc %p\n",
      	     comp, components->size(),
      	     (*components)[comp]->table_, dhts.size(), ac_dht, dc_dht);
      throw("Can't find DHT table in JpegDecoder::JpegDecoder " __FILE__);
    }
    dhts_.push_back(dc_dht);
    dhts_.push_back(ac_dht);
  }
  if (dhts_.size() != components->size() * 2)
    throw("dhts_ table size is wrong");
  // The dimensions of the MCUs in pixels.
  const int hq = kBlockSize * mcu_h_;
  const int vq = kBlockSize * mcu_v_;
  dc_values_.resize(components->size(), 0);
  redaction_dc_.resize(components->size(), 0);
  // How many 8x8 blocks there will be in each direction.
  w_blocks_ = mcu_h_ * ((width_ + hq -1)/hq);
  h_blocks_ = mcu_v_ * ((height_ + vq -1)/vq);
  // How many MCUs there are in the image.
  num_mcus_ = (w_blocks_/mcu_h_) * (h_blocks_/ mcu_v_);
  if (debug > 0)
    printf("Expect %d MCUS. %dx%d blocks h:%d v:%d\n",
	   num_mcus_, w_blocks_, h_blocks_, mcu_h_, mcu_v_);
  image_data_.reserve(num_mcus_ * mcu_h_ * mcu_v_);
  int_image_data_.reserve(num_mcus_ * (2 + mcu_h_ * mcu_v_));
}

void JpegDecoder::WriteZeroLength(int which_dht) {
    const int eob = dhts_[which_dht]->eob_symbol_;
    const int eob_len = dhts_[which_dht]->lengths_[eob];
    const unsigned int code = dhts_[which_dht]->codes_[eob];
    if (debug > 2) printf("Redacting this block, %x\n", code);
    InsertBits(code << (32 - eob_len), eob_len);
}

  // Write value out as coded in the given dht.
  // return 0 on success.
  // return >= 1 if the value can't be written. 
int JpegDecoder::WriteValue(int which_dht, int value) {
  if (value == 0) {
    WriteZeroLength(which_dht);
    return 0;
  }
  int absval = abs(value);
  int coded_len = 0;
  while (absval >> coded_len) ++coded_len;
  unsigned int coded_val = absval;
  if (value < 0) {
    coded_val ^= ((1 << coded_len) - 1);
  }
  const int len_index = dhts_[which_dht]->Lookup(coded_len);
  if (len_index < 0)
    return 1;
  const int coded_len_len = dhts_[which_dht]->lengths_[len_index];
  const unsigned int coded_len_code = dhts_[which_dht]->codes_[len_index];
  // printf("Writing value %d in %d + %d bits: %x %x\n", value,
  // 	 coded_len_len, coded_len, coded_len_code, coded_val);
  InsertBits(coded_len_code << (32-coded_len_len), coded_len_len);
  InsertBits(coded_val << (32 - coded_len), coded_len);
  return 0;
}

void JpegDecoder::SetRedactingState(Redaction *redaction) {
  // Determine if we're in or on the edge of.
  region_index_ = InRedactionRegion(redaction);
  if (region_index_ >= 0) {
    redaction_method_ =
      redaction->GetRegion(region_index_).GetRedactionMethod();
    if (redacting_ == kRedactingInactive) {
      redacting_ = kRedactingStarting;  // Start.
      if (current_strip_ != NULL) throw("Strip already exists");
      current_strip_ = new JpegStrip(GetX(mcus_), GetY(mcus_),
				     data_pointer_ - num_bits_, 
				     redaction_bit_pointer_);
    } else {
      redacting_ = kRedactingActive;  // Steady state redacting.
    }
  } else { // Not in redaction region (any more?)
    if (redacting_ == kRedactingStarting ||
	redacting_ == kRedactingActive) {
      redacting_ = kRedactingEnding;  // Transition out.
      //      printf("Transition out at %d\n", mcus_);
    } else {
      redacting_ = kRedactingInactive;  // (Now) in steady state.
    }
  }
}

void JpegDecoder::Decode(Redaction *redaction) {
  redaction_ = redaction;
  ResetDecoding();
  if (redaction != NULL && redaction->NumRegions() > 0) {
    redacting_ = kRedactingInactive;
    // Reserve space for the redacted data- should be smaller than the original.
    redacted_data_.reserve(((length_ + 7) >> 3) + 2); // For end marker later.
  }

  while (mcus_ < num_mcus_) {
    if (redacting_ != kRedactingOff) {
      SetRedactingState(redaction);
    }
    // Find the matching symbol.
    DecodeOneMCU();
    ++mcus_;
    // When stopping redacting, 
    // for simplicity we actually store the whole MCU,
    // even though most of it is unchanged. (Interleaved components
    // mean the changed bits are spread among the unchanged portions).
    if (redacting_ == kRedactingEnding)
      StoreEndOfStrip(redaction);
  }

  // Terminating when still active.
  if (redacting_ == kRedactingActive)
      StoreEndOfStrip(redaction);

  if (debug > 0)
    printf("Got to %d mcus. %d bits left.\n", num_mcus_,
	   length_ - data_pointer_ + num_bits_);
  length_ = redaction_bit_pointer_;
  redaction_ = NULL;
}

void JpegDecoder::StoreEndOfStrip(Redaction *redaction) {
  //      printf("Endstrip %d %d\n", subblock, mcus_);
  if (current_strip_ == NULL)
    throw("Ending redaction but no strip");
  current_strip_->SetSrcEnd(data_pointer_ - num_bits_, mcus_);
  current_strip_->SetDestEnd(data_, redaction_bit_pointer_);
  //  printf("Adding a strip\n");
  redaction->AddStrip(current_strip_);
  current_strip_ = NULL;
}

void JpegDecoder::DecodeOneMCU() {
  const int num_components = (*components_).size();
  for (int comp = 0; comp < num_components; ++comp) {
    int vf = (*components_)[comp]->v_factor_;
    int hf = (*components_)[comp]->h_factor_;
    int dht = (*components_)[comp]->table_;
    if (debug > 2)
      printf("Decoding MCU %d,%d DHT: %d %dx%d\n", mcus_, dht, comp, hf, vf);
    for (int v = 0; v < vf; ++v) {
      for (int h = 0; h < hf; ++h) {
	const int subblock = v + h * vf;
	// redaction & subblock_redaction encode the state transitions,
	// but these need to be different on the block level from the MCU.
	int subblock_redaction = redacting_;
	if (redacting_ == kRedactingStarting && subblock != 0)
	  subblock_redaction = kRedactingActive;
	// if (redacting_ == kRedactingEnding && subblock != 0)
	//   subblock_redaction = kRedactingInactive;

	int dc_value = 0;
	try {
	  dc_value = DecodeOneBlock(dht, comp, subblock_redaction);
	} catch (const char *error) {
	  fprintf(stderr,
		  "DecodeOneMCU Caught %s at MCU %d of %d\n",
		  error, mcus_, num_mcus_);
	  throw(error);
	}

	if ((*components_)[comp]->table_ == 0) { // Y component
	  if (debug > 2)
	    printf("DCY: %d\n", dc_value);
	  //	  y_value_ += dc_value;
	  while (dc_values_[0] < (-128 << dct_gain_) ||
		 dc_values_[0] >= (128 << dct_gain_)) {
	    ++dct_gain_;
	    if (debug > 0)
	      printf("MCU %d dc_value %d y_value_ %d. Doubling gain to %d\n",
		     mcus_, dc_value, dc_values_[0], dct_gain_);
	    for (int op = 0; op < image_data_.size(); ++op)
	      image_data_[op]= image_data_[op]/2 + 64;
	  }
	  image_data_.push_back((dc_values_[0] + (128 << dct_gain_))
				>> dct_gain_);
	}
      }
    }
  }
}

  // Work out the int_image_data to be used for this pixel.
  int JpegDecoder::LookupPixellationValue(int comp) {
    // Each MCU contains this many components.
    const int mcu_size = mcu_h_ * mcu_v_ + 2;
    // The first block of the Y (0), or the only block of U (1), V (2).
    int component = (comp == 0) ? 0 : comp += mcu_size - 3;
    
    // decode the blockindex from mcus_ ....
    // How many MCUs wide the image is.
    const int mcu_width = w_blocks_ / mcu_h_;
    // The MCU coordinate of the current MCU.
    const int x = mcus_ % mcu_width;
    const int y = mcus_ / mcu_width;
    
    // Quantize to a particular number of MCUs per megapixel.
    // Dimension of a mega pixel in MCUs. Default to 4 for background.
    int megapixel_size = 3;
    int megapixels_per_region = 12;
    if (redaction_method_ != Redaction::redact_inverse_pixellate) {
      int w_size = (redaction_->GetRegion(region_index_).GetWidth() /
		    megapixels_per_region + (8 * mcu_h_) -1) / (8 * mcu_h_);
      int h_size = (redaction_->GetRegion(region_index_).GetHeight() /
		    megapixels_per_region + (8 * mcu_v_) -1) / (8 * mcu_v_);
      megapixel_size = h_size;
      if (w_size > h_size) megapixel_size = w_size;
    }
    const int blockindex = (x / megapixel_size) * megapixel_size +
      mcu_width * (y / megapixel_size) * megapixel_size;
    
    int return_value = int_image_data_[blockindex * mcu_size + component];
    return return_value;
  }


// Decode one 8x8 block using the specified Huffman Table.
// redacting is a flag showing the current state
// starting/ending/redacting/inactive
// We write DC with table 2 * dht and AC with table 2 * dht + 1
// When not redacting we just copybits.
// When redacting we can write other content.
int JpegDecoder::DecodeOneBlock(int dht, int comp, int redacting) {
  if (num_bits_ <= 16)
    FillBits();
  unsigned int dc_symbol_size; // The number of bits to encode the symbol.
  int dc_length_size = 0; // the number of bits to encode the symbol length.
  try {
    dc_length_size = dhts_[2*dht]->Decode(current_bits_, num_bits_,
					  &dc_symbol_size);
  } catch (const char *error) {
    fprintf(stderr, "Caught %s at MCU %d of %d\n", error, mcus_, num_mcus_);
    throw(error);
  }
  if (redacting == kRedactingInactive) CopyBits(dc_length_size);
  DropBits(dc_length_size);
  if (num_bits_ < dc_symbol_size)
    FillBits();
  if (redacting == kRedactingInactive) CopyBits(dc_symbol_size);
  const int dc_value = NextValue(dc_symbol_size);
  // Current cumulative value for this pixel.
  dc_values_[comp] += dc_value;
  int_image_data_.push_back(dc_values_[comp]);
  
  if (redaction_ && redacting != kRedactingInactive) {
  // Work out the (absolute) value we want to write.
  // Default is the cumulative sum so far.
    int value_to_write = dc_values_[comp];
    // If solid, we write out 0.
    if ( redacting != kRedactingEnding) {
      if (redaction_method_ == Redaction::redact_solid) {
	// Write black.
	//	value_to_write = (comp == 0) ? (-127 * (1 << dct_gain_)) : 0;
	value_to_write = (comp == 0) ? (-127 * (1 << dct_gain_)) : ((comp==1) ? 0 : 0);
	//	value_to_write = (comp == 0) ? (0 * (1 << dct_gain_)) : ((comp==1) ? 0 : 0);
      }
      else if (redaction_method_ == Redaction::redact_copystrip)
	value_to_write = redaction_dc_[comp];
      else if (redaction_method_ == Redaction::redact_pixellate ||
	       redaction_method_ == Redaction::redact_inverse_pixellate)
	value_to_write = LookupPixellationValue(comp);
    }
    int rv;
    while (rv = WriteValue(2 * dht, value_to_write - redaction_dc_[comp]) != 0) {
      int delta = value_to_write - redaction_dc_[comp];
      value_to_write = delta *.9 + redaction_dc_[comp];
    }
    // if (rv != 0) {
    //   fprintf(stderr,
    // 	  "Caught error in WriteValue for comp %d, table 2*%d, value %d=%d-%d.",
    // 	      comp, dht, value_to_write-redaction_dc_[comp],
    // 	      value_to_write, redaction_dc_[comp]);
    //   throw("WriteValueErr");
    // }
    redaction_dc_[comp] = value_to_write;
  }
  if (redacting == kRedactingInactive)
    redaction_dc_[comp] = dc_values_[comp];

  // Now deal with AC.
  int coeffs = 1; // DC is the first
  // If we're redacting, have no AC (otherwise copy AC later).
  if (redacting == kRedactingActive || redacting == kRedactingStarting)
    WriteZeroLength(2 * dht + 1);

  for (; coeffs <= 63;) {
    int start_coeff = coeffs;
    unsigned int ac_symbol;
    if (num_bits_ <= 16)
      FillBits();
    int ac_length =
      dhts_[2*dht + 1]->Decode(current_bits_, num_bits_, &ac_symbol);
    const int zero_run_length = ac_symbol >>4;
    ac_symbol  &= 0xf;
    if (redacting == kRedactingInactive || redacting == kRedactingEnding)
      CopyBits(ac_length);
    DropBits(ac_length);
    // If symbol is (15,0) there's no  value, but we skip 16.
    coeffs += zero_run_length;
    coeffs++;
    if (debug >=2)
      printf("AC %d-%d Zrun %d len %d sym %u\n",
	     start_coeff, coeffs-1, zero_run_length, ac_length, ac_symbol);
    if (ac_symbol == 0 && zero_run_length == 0) break;
    if (num_bits_ < ac_symbol)
      FillBits();
    if (redacting == kRedactingInactive || redacting == kRedactingEnding)
      CopyBits(ac_symbol);
    DropBits(ac_symbol); // Could actually decode the value here.
  }
  if (debug >=2 && coeffs >= 63) printf("EOB64\n");
  if (debug > 2)
    printf("MCU %d data %d. %d coeffs\n",
	   mcus_, data_pointer_, coeffs);
  return dc_value;
}

// Get a DC value of length len bits from the current_bits & drop those bits.
int JpegDecoder::NextValue(int len) {
  if (len == 0) return 0;
  unsigned int val = current_bits_ >> (word_size_ - len);
  DropBits(len);
  //  std::string s = Binary(val, len);
  unsigned int mask = 1<< (len-1);
  if (val & mask) // Top bit set: positive number.
    return val;
  else
    return (-(mask << 1) + val + 1);
}
}  // namespace jpeg_redaction
