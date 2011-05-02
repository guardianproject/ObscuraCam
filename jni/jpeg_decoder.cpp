#include <stdio.h>
#include "jpeg_decoder.h"
#include "jpeg.h"
#include "jpeg_dht.h"

namespace jpeg_redaction {
JpegDecoder::JpegDecoder(int w, int h,
			 unsigned char *data,
			 int length,
			 const std::vector<JpegDHT *> &dhts,
			 const std::vector<Jpeg::JpegComponent*> *components) :
  height_(h), width_(w), components_(components) {
  data_ = data;
  length_ = length;
  redacting_ = 0;
  ResetDecoding();
  image_data_.reserve((height_/8) * (width_/8));
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
	  printf("Comp %d %s DHT: %d\n",
		 comp, (dhts[i]->class_? "AC":"DC"), i);
	if (dhts[i]->class_ == 0)
	  dc_dht = dhts[i];
	else
	  ac_dht = dhts[i];
      }
    }
    if (ac_dht == NULL || dc_dht == NULL) throw("Can't find table ");
    dhts_.push_back(dc_dht);
    dhts_.push_back(ac_dht);
  }
  if (dhts_.size() != components->size() * 2)
    throw("dhts_ table size is wrong");
  const int hq = 8 * mcu_h_;
  const int vq = 8 * mcu_v_;
  w_blocks_ = mcu_h_ * ((width_ + hq -1)/hq);
  h_blocks_ = mcu_v_ * ((height_ + vq -1)/vq);
  num_mcus_ = (w_blocks_/mcu_h_) * (h_blocks_/ mcu_v_);
  printf("Expect %d MCUS. %dx%d blocks h:%d v:%d\n",
	 num_mcus_, w_blocks_, h_blocks_, mcu_h_, mcu_v_);
}

void JpegDecoder::WriteZeroLength(int which_dht) {
    const int eob = dhts_[which_dht]->eob_symbol_;
    const int eob_len = dhts_[which_dht]->lengths_[eob];
    const unsigned int code = dhts_[which_dht]->codes_[eob];
    if (debug > 0) printf("Redacting this block, %x\n", code);
    InsertBits(code << (32 - eob_len), eob_len);
}

void JpegDecoder::WriteValue(int which_dht, int value) {
  if (value == 0) {
    WriteZeroLength(which_dht);
    return;
  }
  int absval = abs(value);
  int coded_len = 0;
  while (absval >> coded_len) ++coded_len;
  unsigned int coded_val = absval;
  if (value < 0) {
    coded_val ^= ((1 << coded_len) - 1);
  }
  int len_index = dhts_[which_dht]->Lookup(coded_len);
  int coded_len_len = dhts_[which_dht]->lengths_[len_index];
  unsigned int coded_len_code = dhts_[which_dht]->codes_[len_index];
  // printf("Writing value %d in %d + %d bits: %x %x\n", value,
  // 	 coded_len_len, coded_len, coded_len_code, coded_val);
  InsertBits(coded_len_code << (32-coded_len_len), coded_len_len);
  InsertBits(coded_val << (32 - coded_len), coded_len);
}

// Decode one 8x8 block using the specified Huffman Table.
int JpegDecoder::DecodeOneBlock(int dht, int comp, int redaction) {
  if (num_bits_ <= 16)
    FillBits();
  unsigned int dc_symbol;
  int dc_length = 0;
  try {
    dc_length = dhts_[2*dht]->Decode(current_bits_, num_bits_, &dc_symbol);
  } catch (const char *error) {
    printf("Caught %s at MCU %d of %d\n", error, mcus_, num_mcus_);
    throw(error);
  }
  if (redaction == 1 || redaction == 3)
    CopyBits(dc_length);
  if (redaction == 2) { // Write out zero.
    // printf("Writing zero %d\n", comp);
    WriteZeroLength(2 * dht);
  }
  DropBits(dc_length);
  if (num_bits_ < dc_symbol)
    FillBits();
  //  printf("Sym\n");
  if (redaction == 1 || redaction == 3)
    CopyBits(dc_symbol);
  const int dc_value = NextValue(dc_symbol);
  // if (redaction == 2 || redaction == 3)
  //   WriteValue(2*dht, dc_value);
  // Maintain the dc delta through the redaction.
  if (redaction == 3)
    redaction_dc_[comp] = 0;
  if (redaction == 2)
    redaction_dc_[comp] += dc_value;
  if (redaction == 5) {  // Write out new delta.
    redaction_dc_[comp] += dc_value;
    WriteValue(2 * dht, redaction_dc_[comp]);
  }
  if (debug > 0)
    printf("DCValue is %d\n", dc_value);
  int coeffs = 1; // DC is the first
  // If we're redacting, have no AC
  if (redaction == 2 || redaction == 3) {
    WriteZeroLength(2 * dht + 1);
  }

  for  (; coeffs <= 63;) {
    int start_coeff = coeffs;
    unsigned int ac_symbol;
    if (num_bits_ <= 16)
      FillBits();
    int ac_length =
      dhts_[2*dht + 1]->Decode(current_bits_, num_bits_, &ac_symbol);
    const int zero_run_length = ac_symbol >>4;
    ac_symbol  &= 0xf;
    if (redaction == 1 || redaction == 5) CopyBits(ac_length);
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
    if (redaction == 1 || redaction == 5) CopyBits(ac_symbol);
    DropBits(ac_symbol); // Could actually decode the value here.
  }
  if (debug >=2 && coeffs >= 63) printf("EOB64\n");
  if (debug > 0)
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
