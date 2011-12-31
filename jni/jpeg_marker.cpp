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


// jpeg_marker.cpp: implementation of the JpegMarker class to store
// one marker from a JPEG file.

#include <stdio.h>
#include "byte_swapping.h"
#include "debug_flag.h"
#include "jpeg.h"
#include "jpeg_marker.h"

namespace jpeg_redaction {
  // Save this (preloaded) marker to disk.
  int JpegMarker::Save(FILE *pFile) {
    const int location = ftell(pFile);
    if (pFile == NULL)
      throw("Null File in JpegMarker::Save.");
    unsigned short markerswapped = byteswap2(marker_);
    int rv = fwrite(&markerswapped, sizeof(unsigned short), 1, pFile);
    if (rv != 1) return 0;
    unsigned short slice_or_length = length_;
    if (marker_ == Jpeg::jpeg_sos)
      slice_or_length = slice_;
    ByteSwapInPlace(&slice_or_length, 1);
    rv = fwrite(&slice_or_length, sizeof(unsigned short), 1, pFile);
    if (rv != 1) return 0;
    if (marker_ == Jpeg::jpeg_sos) {
      WriteWithStuffBytes(pFile);
      // Add the EOI
      unsigned char eoi0 = 0xff;
      unsigned char eoi1 = 0xd9;
      fwrite(&eoi0, sizeof(eoi0), 1, pFile);
      fwrite(&eoi1, sizeof(eoi1), 1, pFile);
    } else {
      rv = fwrite(&data_[0], sizeof(char), data_.size(), pFile);
      if (rv != data_.size()) return 0;
    }
    if (debug > 0)
      printf("Saved marker %04x length %u at %d\n", marker_, length_, location);
    return 1;
  }
  // Write out the marker inserting stuff (0) bytes when there's an ff.
  void JpegMarker::WriteWithStuffBytes(FILE *pFile) {
    int written = 0; // Next byte to write out.
    int check = 10;  // Next byte to check.
    int rv = 0;
    unsigned char zero = 0x00;
    int stuff_bytes = 0;
    // Repeatedly look for the next ff.
    // Then write all the data up to & including it and write out the
    // stuff byte (00).
    if (check > data_.size()) {
      fprintf(stderr, "data size is %zu\n", data_.size());
      throw("data too short in stuffing.");
    }
    while (check < data_.size()) {
      if (data_[check] == 0xff) {
	rv = fwrite(&data_[written], sizeof(char), check + 1 - written, pFile);
	if (rv != check + 1 - written) 
	  throw("Failed to write enough bytes in WriteWithStuffBytes");
	rv = fwrite(&zero, sizeof(char), 1, pFile);
	if (rv != 1) 
	  throw("Failed to write stuffbyte in WriteWithStuffBytes");
	written = check + 1;
	++stuff_bytes;
      }
      ++check;
    }
    if (check != data_.size()) throw("data_.size() mismatch in stuffing.");
    // Write out the last chunk of data.
    if (written < check)
      rv = fwrite(&data_[written], sizeof(char), check - written, pFile);
    if (debug > 0)
      printf("Inserted %d stuff_bytes in %zu now %zu\n", stuff_bytes,
	     data_.size(), data_.size() + stuff_bytes);
  }
  void JpegMarker::RemoveStuffBytes() {
    if (data_.size() != length_ - 2) {
      fprintf(stderr, "Data %zu len %d\n", data_.size(), length_);
      throw("Data length mismatch in RemoveStuffBytes");
    }
    const int start_of_huffman = 10;
    if (data_.size() < start_of_huffman) {
      fprintf(stderr, "Data %zu len %d\n", data_.size(), length_);
      throw("Data too short in RemoveStuffBytes");
    }

    int dest = start_of_huffman;
    int src  = start_of_huffman;
    int stuff_bytes = 0;
    for (int src = start_of_huffman; src < length_ - 2; ++dest, ++src) {
      data_[dest] = data_[src];
      if (data_[src] == 0xff && data_[src+1] == 0x00) {
	++src;
	++stuff_bytes;
      }
    }
    if (debug > 0)
      printf("Removed %d stuff_bytes in %zu now %zu\n",
	     stuff_bytes, data_.size(), data_.size() - stuff_bytes);
    length_ -= stuff_bytes;
    data_.resize(data_.size() - stuff_bytes);
  }
} // namespace jpeg_redaction
