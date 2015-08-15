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


#ifndef INCLUDE_BYTE_SWAPPING
#define INCLUDE_BYTE_SWAPPING

// Returns true if current machine is big endian (Motorola)
// vs little endian (Intel)
bool ArchBigEndian();

// General byte swapping functions.
// Swap a contiguous block of num objects of a particular type.
void ByteSwapInPlace(unsigned short *d, int num);
void ByteSwapInPlace(unsigned int *d, int num);
void ByteSwapInPlace(short *d, int num);
void ByteSwapInPlace(int *d, int num);
// Block num objects of size typelen
void ByteSwapInPlace(unsigned char *d, int num, int typelen);

#define byteswap2(x) (((x & 0xff)<<8) | ((x & 0xff00)>>8))
#define byteswap4(x) (((x & 0xff)<<24) | ((x & 0xff00)<<8) | ((x & 0xff0000)>>8) | ((x & 0xff000000)>>24))

#endif // INCLUDE_BYTE_SWAPPING
