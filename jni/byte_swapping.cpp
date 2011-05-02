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
