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


#ifndef INCLUDE_JPEGDHT
#define INCLUDE_JPEGDHT

#include <string>

namespace jpeg_redaction {
extern int debug;
inline std::string Binary(unsigned int b, int length) {
  if (debug == 0)
    throw ("Shouldn't be calling Binary with debug == 0");
  std::string s;
  int l = 0;
  while(l < length) {
    std::string s2((b&1)?"1":"0");
    s = s2 + s;
    if (l%8 == 7) {
      std::string s3(" ");
      s = s3 + s;
    }
    b >>= 1;
    ++l;
  }
  return s;
}

class JpegDHT {
 public:
  JpegDHT() {
    lut_len_ = 0;
    eob_symbol_ = 0;
  }
  virtual ~JpegDHT() {}
  void PrintTable() {
    // print out the table.
    int sym = 0;
    printf("DHT %d%d\n", class_, id_);
    for (int i = 1; i<=16; ++i) {
      if (num_symbols_[i-1] > 0) {
	printf("Length %2d, %3d symbols: ", i, num_symbols_[i-1]);
	for (int n = 0; n < num_symbols_[i-1]; ++n, ++sym) {
	  if (0) {
	    printf("length %d code %d sym %d\n",
		   codes_[sym], lengths_[sym], symbols_[sym]);
	    std::string binary = Binary(codes_[sym], lengths_[sym]);
	    printf("%s,%x ", binary.c_str(), symbols_[sym]);
	  }
	}
	printf("\n");
      }
    }
  }

  // Make a lookup table of 'bits' bits.
  void BuildLUT(int bits) {
    if (debug > 0)
      printf("Building lookup table of %d bits.\n", bits);
    int not_found = 0;
    lut_len_ = bits;
    lut_.clear();
    lut_.resize(1 << bits,-1);
    for (int code = 0; code < lut_.size(); ++code) {
      bool found = false;
      for (int i = 0; found == false && i < lengths_.size(); ++i) {
	if (lengths_[i] > bits) {
	  break;
	}
	unsigned int part = code >> (bits -lengths_[i]);
	if (part == codes_[i]) {
	  lut_[code] = i;
	  found = true;
	}
      }
      if (!found) ++not_found;
    }
    printf("Didn't find %d\n", not_found);
  }
  // Build one DHT from a block of data.
  int Build(unsigned char *data, int bytes_left) {
    // Get the class and ID of this table.
    int bytes_used = 0;
    int lut_len = 0;
    const int max_lut_len = 10;
    class_ = data[0]>>4;
    id_ = data[0] & 0xf;
    bytes_used++;
    // 16 bytes encoding the number of symbols of each length 1..16 bits
    num_symbols_.assign(data + 1, data + 17);
    bytes_used += 16;

    // Now the symbols.
    // The bit std::strings are derived by expanding a binary tree and at each
    // depth, assigning the smallest available codes to the N symbols given
    // in the next block of data. N for each length is given in the table above.
    std::vector<unsigned int> prefixes;
    prefixes.push_back(0);
    for (int length = 1; length <= 16; ++length) {
      // For each prefix pop and assign or
      std::vector<unsigned int> new_prefixes;
      int symbols_used = 0;
      if (num_symbols_[length-1] > 0 && length <= max_lut_len)
	lut_len = length;
      for (int prefix = 0; prefix < prefixes.size(); ++prefix) {
	// Generate left & right symbols.
	for (int suffix = 0; suffix <2; ++suffix) {
	  unsigned int code = (prefixes[prefix] << 1) + suffix;
	  // Determine if we have symbols to assign, or make a new prefix.
	  if (symbols_used < num_symbols_[length-1]) {
	    lengths_.push_back(length);
	    codes_.push_back(code);
	    if (bytes_used > bytes_left) {
	      fprintf(stderr, "No more bytes left in DHT\n");
	      return bytes_used;
	    }
	    if (data[bytes_used] == 0)
	      eob_symbol_ = symbols_.size();
	    symbols_.push_back(data[bytes_used++]);
	    if (symbols_.size() > 256) {
	      fprintf(stderr, "Too many symbols defined\n");
	      throw("too many symbols defined");
	    }
	    ++symbols_used;
	  } else {
	    new_prefixes.push_back(code);
	  }
	}
      }
      prefixes.assign(new_prefixes.begin(), new_prefixes.end());
    }
    // PrintTable();
    BuildLUT(lut_len);
    return bytes_used;
  }

  int Decode(unsigned int current_bits,
	     const int bits_available,
	     unsigned int *symbol) {
    if (debug > 3) {
      std::string allbits = Binary(current_bits, 32);
      printf("Decoding from %s\n", allbits.c_str());
    }
    if (bits_available <= 0 || bits_available > 32) {
      printf("Only %d bits left\n", bits_available);
      throw("Bad number of bits left");
    }
    unsigned int lut_code = current_bits >> (32 - lut_len_);
    int code_index = lut_[lut_code];
    if (code_index >= 0) {
	*symbol = symbols_[code_index];
	if (debug >= 2) {
	  std::string bin = Binary(lut_code, lut_len_);
	  printf("LUTDecoding %d bits of %s as %u\n",
			       lengths_[code_index], bin.c_str(), *symbol);
	}
	return lengths_[code_index];
    }
    if (debug >= 3) {
      std::string s = Binary(current_bits, 32);
      printf("Table %d%d Trying to decode %s / %04x\n",
	     class_, id_, s.c_str(), current_bits);
    }
    for (int i = 0; i < lengths_.size(); ++i) {
      if (lengths_[i] > bits_available) {
	printf("Can't decode with only %d bits left\n", bits_available);
	throw("not enough bits left");
      }
      unsigned int part = current_bits >> (32 -lengths_[i]);
      if (part == codes_[i]) {
	*symbol = symbols_[i];
	if (debug >= 2) {
	std::string bin = Binary(part, lengths_[i]);
	printf("Decoding %s as %u\n", bin.c_str(), *symbol);
	}
	return lengths_[i];
      }
    }
    std::string bin = Binary(current_bits, 32);
    printf("Can't decode %s in table %d%d. %d bits left.\n",
	   bin.c_str(), class_, id_, bits_available);
    throw("can't decode");
  }
  // Find the table entry that codes a particular value.
  // Return -1 if not in the table.
  int Lookup(int value) {
    for (int i = 0; i < symbols_.size(); ++i) {
      if (symbols_[i] == value)
	return i;
    }
    if (debug > 0)
      fprintf(stderr, "Can't find value %d\n", value);
    return -1;
  }
  // Class is 0 for DC, 1 for AC.
  int class_;
  // Table number, as referenced by SOF.
  int id_;
  int eob_symbol_;
  std::vector<int> num_symbols_;
  std::vector<int> lengths_;
  std::vector<unsigned int> codes_;
  std::vector<unsigned int> symbols_;
  // Look up table - for a given bit pattern, which code is this.
  // -1 if more than N (typ 8) bits.
  int lut_len_;
  std::vector<int> lut_;
};
} // namespace redaction

#endif // INCLUDE_JPEGDHT
