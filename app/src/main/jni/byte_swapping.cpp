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


#include "byte_swapping.h"

bool ArchBigEndian() {
  short s = 256;
  return (*(unsigned char*)&s);
}

void ByteSwapInPlace(unsigned short *d, int num) {
  unsigned char *db = (unsigned char *)d;
  while (--num >= 0) {
    unsigned char c = *db;
    *db = *(db + 1);
    *(db+1) = c;
    db += sizeof(*d);
  }
}
void ByteSwapInPlace(unsigned int *d, int num) {
  unsigned char *db = (unsigned char *)d;
  while (--num >= 0) {
    unsigned char c = *db;
    *db = *(db + 3);
    *(db+3) = c;
    c = *(db+1);
    *(db +1) = *(db + 2);
    *(db+2) = c;
    db += sizeof(*d);
  }
}
void ByteSwapInPlace(short *d, int num) {
  ByteSwapInPlace((unsigned short *)d, num);
}
void ByteSwapInPlace(int *d, int num) {
  ByteSwapInPlace((unsigned int *)d, num);
}
void ByteSwapInPlace(unsigned char *d, int num, int typelen) {
  if (typelen == 1)
    return;
  else  if (typelen == 2)
    ByteSwapInPlace((unsigned short *)d, num);
  else if (typelen == 4)
    ByteSwapInPlace((unsigned int *)d, num);
  else if (typelen == 8) { // Tiff specific: rational
    ByteSwapInPlace((unsigned int *)d, num);
    ByteSwapInPlace(((unsigned int *)d)+1, num);
  }
  else throw("Cant swap this length.");
}
