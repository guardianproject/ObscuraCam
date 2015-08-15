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


// Class to apply bit operations to a (JPEG) bit stream.
#ifndef INCLUDE_JPEG_REDACTION_LIBRARY_BIT_SHIFTS
#define INCLUDE_JPEG_REDACTION_LIBRARY_BIT_SHIFTS
#include "debug_flag.h"

namespace jpeg_redaction {
class BitShifts {
public:
  // Shift the block after the  start-th bit up by shift,
  // increasing the size of data.
  static bool ShiftTail(std::vector<unsigned char> *data,
			int *data_bits,
			int start,
			int shift) {
    if (shift < 0)
      throw("ShiftTail: shift is negative");

    if (debug > 0)
      printf("Shifting data (%d bits in %zu bytes) up by %d bits "
	   "starting at b%d B%d. New length %d\n",
	   *data_bits, data->size(), shift, start,
	   start/8, *data_bits + shift);

    // Enough space for everything.
    *data_bits += shift;
    data->resize((*data_bits + 7) /8);
    
    // Shift up the end bits (check the ending?)
    // This is Extremely approximate
    //    (*data)[data->size()-1] = 0;
    int bit_shift = shift % 8;
    const unsigned char low_mask = (1 << bit_shift) -1;
    const unsigned char high_mask = 0xff - low_mask;
    // The byte where the last bit is located.
    const int last_byte = (*data_bits - 1) / 8;
    const int first_byte = (start + shift) / 8;
    // The byte where the very last bit in the last byte would come from.
    int src_byte = (last_byte * 8 + 7 - shift) / 8;
    // Go through all the destination bytes i and pull their bits
    // from the two source bytes.
    unsigned char byte;
    for (int i = last_byte; i >= first_byte; --i, --src_byte) {
      byte  = ((*data)[src_byte] & high_mask) >> bit_shift;
      if (src_byte > 0)
	byte |= ((*data)[src_byte - 1] & low_mask) << (8 - bit_shift);
      if (i == first_byte) {
	// Only affect the lowest start_offset bits.
	const int start_offset = 8 - ((start + shift) % 8);
	const unsigned char final_low_mask = (1 << start_offset) -1;
	const unsigned char final_high_mask = 0xff - final_low_mask;
	if (debug > 0)
	  printf("fb %d flm %d fhm %d so %d\n",
		 i, final_low_mask, final_high_mask, start_offset);
	(*data)[i] = ((*data)[i] & final_high_mask) | (byte & final_low_mask);
      } else {
	(*data)[i] = byte;
      }
    }
    if (debug > 0)
      printf("Ended with src_byte %d\n", src_byte);
    return true;
  }
  // Take the first insert_length bits from insertion
  // copy them to data at position start, overwriting old bits.
  static bool Overwrite(std::vector<unsigned char> *data,
		      int length, 
		      int start,
		      const std::vector<unsigned char> &overwrite,
		      int overwrite_length) {
    if (overwrite_length + start > length)
      throw("Length mismatch in Overwrite");
    if (length > 8 * data->size())
      throw("Data length error in Overwrite");
    if (overwrite_length > 8 * overwrite.size())
      throw("Overwrite length error in Overwrite");
    int start_byte = start / 8;
    // Byte with the last bit in it.
    int end_byte = (start + overwrite_length -1) / 8;
    // How much the overwrite has to be shifted up to match the start.
    int bit_shift = start % 8;
    // The high bit_shift bits.
    const unsigned char low_mask = (1 << (8 - bit_shift)) -1;
    const unsigned char high_mask = 0xff - low_mask;
    int ov = 0;
    for (int i = start_byte; i <= end_byte; ++i, ++ov) {
      unsigned char byte = 0;
      if (i == start_byte)
	byte = (*data)[i] & high_mask;
      else
	byte = (overwrite[ov-1] << (8 - bit_shift)) & high_mask;
      byte += (overwrite[ov] >> bit_shift);
      if (i == end_byte) {
	const int shift_final = 1 + (start + overwrite_length - 1) % 8;
	const unsigned char low_mask_final = (1 << (8 - shift_final)) -1;
	const unsigned char high_mask_final = 0xff - low_mask_final;
	if (debug > 0)
	  printf("i %d shift_final %d mask h%0x l%x\n",
		 i, shift_final, high_mask_final, low_mask_final);
	byte = (byte & high_mask_final) | ((*data)[i] & low_mask_final);
      }
      (*data)[i] = byte;
    }
    return true;
  }
  // Take the first insert_length bits from insertion
  // copy them to data at position start, shifting up old bits.
  static bool Insert(std::vector<unsigned char> *data,
		     int *length, 
		     int start,
		     const std::vector<unsigned char> &insertion,
		     int insert_length) {
    ShiftTail(data, length, start, insert_length);
    return Overwrite(data, *length, start, insertion, insert_length);
  }
  // Pad the last byte with ones.
  static int PadLastByte(std::vector<unsigned char> *data, int bits) {
    if (bits > data->size() * 8) throw("too many bits in PadLastByte");
    int used_bits_last_byte = ((bits - 1) % 8) + 1;
    if (used_bits_last_byte != 8) {
      unsigned char unused_bits_mask = (1 << (8 - used_bits_last_byte)) - 1;
      if (debug > 0)
	printf("padding %d bits mask %02x\n",
	       8-used_bits_last_byte, unused_bits_mask);
      data->back() |= unused_bits_mask;
    }
  }
}; 
}  // namespace jpeg_redaction
#endif  // INCLUDE_JPEG_REDACTION_LIBRARY_BIT_SHIFTS

