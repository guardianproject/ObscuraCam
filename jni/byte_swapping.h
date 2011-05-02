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
