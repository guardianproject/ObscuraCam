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

#ifndef INCLUDE_JPEG_MARKER
#define INCLUDE_JPEG_MARKER

#include <vector>

namespace jpeg_redaction {
class JpegMarker {
 public:
  // length_ is payload size- includes storage for length itself.
  // The actual data_ buffer is of size length_ - 2
  JpegMarker(unsigned short marker, unsigned int location,
	     int length) {
    marker_ = marker;
    location_ = location;
    length_ = length;
  }
  // Create a marker from a block of data of given length.
  JpegMarker(unsigned short marker,
	     const unsigned char *data,
	     unsigned int length) {
    marker_ = marker;
    location_ = 0;
    length_ = length + 2;
    data_.resize(length);
    memcpy(&data_.front(), data, length);
    bit_length_ = length * 8;
  }
  void LoadFromLocation(FILE *pFile) {
    fseek(pFile, location_ + 4, SEEK_SET);
    LoadHere(pFile);
  }
  // Print a summary of the marker.
  void Print() const {
    printf("Marker %x length %d in bits %d datalen %zu location %d.\n",
	   marker_, length_, bit_length_, data_.size(), location_);
  }
  void LoadHere(FILE *pFile) {
    data_.resize(length_-2);
    int rv = fread(&data_[0], sizeof(char), length_-2, pFile);
    bit_length_ = 8 * (length_ - 2);
    if (rv  != length_-2) {
      printf("Failed to read marker %x at %d\n", marker_, length_);
      throw("Failed to read marker");
    }
  }
  int GetBitLength() const { return bit_length_; }
  void SetBitLength(int bit_length) {
    if (bit_length > data_.size() * 8)
      throw("Setting bit length longer than buffer");
    bit_length_ = bit_length;
  }
  void WriteWithStuffBytes(FILE *pFile);
  void RemoveStuffBytes();
  int Save(FILE *pFile);
  unsigned short slice_;
  // Length is payload size- includes storage for length itself.
  int length_;
  unsigned int location_;
  unsigned short marker_;
  int bit_length_;
  std::vector<unsigned char> data_;
};  // JpegMarker
}  // namespace redaction

#endif // INCLUDE_JPEG_MARKER
