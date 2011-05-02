// jpeg.cpp: implementation of the Jpeg class.
//
//////////////////////////////////////////////////////////////////////

#include "jpeg.h"
#include "jpeg_dht.h"
#include "jpeg_decoder.h"
#include "redaction.h"
#include "photoshop_3block.h"
#include "byte_swapping.h"

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////

namespace jpeg_redaction {
int debug = 0;

void DumpHex(unsigned char *data, int len) {
  for (int i = 0; i < len; ++i) {
    printf("%02x ", data[i]);
    if ((i +1) %16 == 0)
      printf("\n");
  }
  if (len %16 != 0)
    printf("\n");
}
Jpeg::Jpeg(char const * const pczFilename, bool loadall) :
  width_(0), height_(0) , softype_(-1), photoshop3_(NULL)
{
  filename_ = pczFilename;
//  exif_ = NULL;

  FILE *pFile = fopen(pczFilename, "rb");
  if (pFile == NULL)
    return;

  LoadFromFile(pFile, loadall, 0);
  fclose(pFile);
}

const char *Jpeg::MarkerName(int marker) const {
  if (marker >= jpeg_app && marker < jpeg_app + 0xf) {
    return "APPn";
  }
  switch ((enum markers)marker) {
  case jpeg_soi: return "SOI"; break;
  case jpeg_sof0: return "SOF0"; break;
  case jpeg_sof2: return "SOF2"; break;
  case jpeg_dht: return "DHT"; break;
  case jpeg_eoi: return "EOI"; break;
  case jpeg_sos: return "SOS"; break;
  case jpeg_dqt: return "DQT"; break;
  case jpeg_dri: return "DRI"; break;
  case jpeg_com: return "COM"; break;
  }
  return "UNKNOWN MARKER";
}

int Jpeg::LoadFromFile(FILE *pFile, bool loadall, int offset) {
  unsigned short marker = 0;
  int iRV = fread(&marker, sizeof(unsigned short), 1, pFile);
  if (iRV != 1)
    throw(-2);
  if (marker != 0xd8ff) throw("Bad JPEG start marker");

  while(!feof(pFile)) {
    const unsigned int blockloc = ftell(pFile);
    int iRV = fread(&marker, sizeof(unsigned short), 1, pFile);
    if (iRV != 1)
      throw(-1);
    marker = byteswap2(marker);
    printf("Got marker %x %s at %d\n", marker, MarkerName(marker), blockloc);

    if (marker == jpeg_eoi) {
      return 0;
      //      continue;
    }
    // if (marker == 0xffff) {
    //   continue;
    // }
    if (marker == jpeg_app + 1) { // App1 is EXIF
      LoadExif(pFile, blockloc, loadall);
      continue;
    }

    if (marker == jpeg_app + 0xd) { // App13 is Photoshop/IPTC
      printf("Photoshop\n");
      unsigned short blocksize;
      unsigned int magic = 0, exifoffset = 0;
     // unsigned short myshort;
      iRV = fread(&blocksize, sizeof(unsigned short), 1, pFile);
      blocksize = byteswap2(blocksize);
      try {
        photoshop3_ = new Photoshop3Block(pFile, blocksize-sizeof(blocksize));
      }  catch (char *ex) {
        printf("Got exception %s", ex);
        throw(ex);
      }
      fseek(pFile, blockloc + blocksize + sizeof(marker), SEEK_SET);
      continue;
    }
    if (marker >= jpeg_app && marker < jpeg_app + 16) {
      unsigned short blocksize;
      iRV = fread(&blocksize, sizeof(unsigned short), 1, pFile);
      blocksize = byteswap2(blocksize);
      AddMarker(marker, blockloc, blocksize, pFile, loadall);
      printf("APP %x unsupported\n", marker);
      //      throw("AppN unsupported");
      continue;
    }
    // http://www.bsdg.org/swag/GRAPHICS/0143.PAS.html
    if (marker == jpeg_sof0 || marker == jpeg_sof2) {
      unsigned short blocksize;
      iRV = fread(&blocksize, sizeof(unsigned short), 1, pFile);
      blocksize = byteswap2(blocksize);
      printf("SOF sz %u nextloc %ld\n",
	     blocksize, blockloc + blocksize + sizeof(marker));
      JpegMarker *sof = AddMarker(marker, blockloc, blocksize, pFile, true);
      softype_ = (marker == jpeg_sof0) ? 0:2;
      unsigned char *data = (unsigned char*)(&sof->data_[0]);
      if (0) {
	DumpHex(data, blocksize);
      }
      const int bits_per_sample = data[0];
      height_ = data[1] * 256 + data[2];
      width_ = data[3] * 256 + data[4];
      int components = data[5];
      if (blocksize != 8 + 3 * components) {
	printf("Error - wrong blocksize ins SOF\n");
      }
      for (int i = 0; i < components; ++i) {
	JpegComponent *comp = new JpegComponent(data + 6 + 3 * i);
	comp->Print();
	components_.push_back(comp);
      }
      printf("JPEG Image is %d x %d\n", width_, height_);
      continue;
    }
    if (marker == jpeg_dqt || marker == jpeg_dht) {
      unsigned short blocksize;
      iRV = fread(&blocksize, sizeof(unsigned short), 1, pFile);
      blocksize = byteswap2(blocksize);
      printf(" sz %d nextloc %d\n", blocksize, blockloc + blocksize + 2);
      JpegMarker *d = AddMarker(marker, blockloc, blocksize, pFile, loadall);
      if (marker == jpeg_dht && loadall) {
	BuildDHTs(d);
      }
      continue;
    }
    if (marker ==  jpeg_dri ) {
      unsigned short blocksize;

      iRV = fread(&blocksize, sizeof(unsigned short), 1, pFile);
      blocksize = byteswap2(blocksize);
      JpegMarker *dri = AddMarker(marker, blockloc, blocksize, pFile, true);
      restartinterval_ = *(short*)(&dri->data_[0]);
      restartinterval_ = byteswap2(restartinterval_);
      printf("Restart interval %d\n", restartinterval_);
      continue;
    }
    if (marker == jpeg_sos) { // Start of scan
      return ReadSOSMarker(pFile, blockloc, loadall);
    }
    throw("Unknown marker found in JPEG");
  }
  return 0;
}

int Jpeg::LoadExif(FILE *pFile, unsigned int blockloc, bool loadall) {
  unsigned short blocksize;

  bool arch_big_endian = ArchBigEndian();
  unsigned int magic = 0, exifoffset = 0;
  unsigned short myshort, forty_two, byte_order;
  int iRV = fread(&blocksize, sizeof(unsigned short), 1, pFile);
  if (!arch_big_endian)
    blocksize = byteswap2(blocksize);
  printf("APP Block size is %d %04x\n", blocksize, blocksize);
  iRV = fread(&magic, sizeof(unsigned int), 1, pFile);
  if (iRV != 1)
    throw(-2);
  if (!arch_big_endian) ByteSwapInPlace(&magic, 1);
  if (magic != 0x45786966) // 'Exif'
    throw(-6);
  iRV = fread(&myshort, sizeof(unsigned short), 1, pFile);
  if (myshort != 0) throw(-4);

  iRV = fread(&byte_order, sizeof(unsigned short), 1, pFile);
  if (iRV != 1)
    throw(-2);
  bool big_endian = false;
  printf("EXIF byte_order: %04x\n", byte_order);
  if (byte_order == 0x4d4d) // "Motorola"
    big_endian = true;
  else if (byte_order != 0x4949) // "Intel"
    throw("Don't recognize EXIF tag");
  printf("EXIF tag is %s endian %d, arch is %s %d\n",
	 (big_endian ? "big" : "little"), big_endian,
	 (arch_big_endian ? "big" : "little"), arch_big_endian);
  bool byte_swapping = (big_endian != arch_big_endian);
  iRV = fread(&forty_two, sizeof(unsigned short), 1, pFile);
  if (iRV != 1)
    throw(-2);
  if (byte_swapping) ByteSwapInPlace(&forty_two, 1);
  if (forty_two != 42)
    throw(-2);
  iRV = fread(&exifoffset, sizeof(unsigned int), 1, pFile);
  if (iRV != 1)
    throw(-2);
  if (byte_swapping) ByteSwapInPlace(&exifoffset, 1);
  unsigned int exifloc = ftell(pFile);
  printf("Exifloc %d/0x%x Offset %d/0x%x\n",
	 exifloc, exifloc, exifoffset, exifoffset);
  // Absolute location of IFD:
  unsigned int ifdoffset = exifloc;
  while (ifdoffset > 0) {
    const unsigned int subfileoffset = blockloc + 10;
    printf("Loading IFD %lu @%u subfileoffset %u swap %d ",
	   ifds_.size(), ifdoffset, subfileoffset, byte_swapping);
    TiffIfd *tempifd = new TiffIfd(pFile, ifdoffset,
				   loadall, subfileoffset, byte_swapping);
    ifds_.push_back(tempifd);
    ifdoffset = tempifd->GetNextIfdOffset();
    if (ifdoffset != 0)
      ifdoffset += subfileoffset;
    printf("Next offset %d\n", ifdoffset);
  }
  //      exif_ = new TiffIfd(pFile, exifloc, true, blockloc + 10); // Need to pass the baseline.
  iRV = fseek(pFile, blockloc + blocksize + sizeof(unsigned short),
	      SEEK_SET);
  return 0;
}

int Jpeg::ReadSOSMarker(FILE *pFile, unsigned int blockloc, bool loadall) {
  short slice = 0;
  int iRV = fread(&slice, sizeof(unsigned short), 1, pFile);
  slice = byteswap2(slice);
  printf("SOS slice %d\n", slice);
  int dataloc = blockloc + sizeof(unsigned short); // marker's size
  unsigned int buf = 0;
  int datalen = 0;

  while (1) { // Read the data looking for markers
    buf <<= 8;
    iRV = fread(&buf, sizeof(unsigned char), 1, pFile);
    if (iRV != 1) {
      printf("Failed to load byte at %d (datalen %d)\n",
	     blockloc + 4 + datalen, datalen);
      throw(-10);
    }
    datalen++;
    if ((buf & 0xff00) == 0xff00 && (buf & 0xffff)!= 0xff00)
      printf("In scan found marker 0x%x\n", (buf & 0xffff));
    if ((buf & 0xffff) == jpeg_eoi) {
      printf("EOI at %d (len %d)\n", blockloc + 4 + datalen, datalen);
      break;
    }
    if (feof(pFile))
      throw("Got to end of file in JPEG SOS\n");
  }
  if (loadall) fseek(pFile, blockloc + 4, SEEK_SET);
  JpegMarker *somarker =
    AddSOMarker(blockloc, datalen, pFile, loadall, slice);
  return 0;
}

Jpeg::~Jpeg() {
  for(int i = 0; i < ifds_.size(); ++i)
    delete ifds_[i];
  ifds_.clear();
  delete photoshop3_;
  for (int i = 0; i < markers_.size(); ++i) {
    delete markers_[i];
  }
  for (int i = 0; i < dhts_.size(); ++i) {
    delete dhts_[i];
  }
  for (int i = 0; i < components_.size(); ++i) {
    delete components_[i];
  }
}

Iptc *Jpeg::GetIptc() {
  if (photoshop3_)
    return photoshop3_->GetIptc();
  else
    return NULL;
}


// Save the image to disk (after preloading)
int Jpeg::Save(const char * const filename) {
  const bool arch_big_endian = ArchBigEndian();
  FILE *pFile = fopen(filename, "w");
  if (pFile == NULL)
    return 0;
  // Write the header,
  unsigned short magic = 0xd8ff;
  if (arch_big_endian) ByteSwapInPlace(&magic, 1);
  int rv = fwrite(&magic, sizeof(unsigned short), 1, pFile);
  bool write_exif = true;

  // write the EXIF IFDs  Code based on CR2.cpp
  if (write_exif && ifds_.size() !=0) {
    std::vector<unsigned int> pending_pointers;  // Pairs of Where/What
    unsigned short marker = jpeg_app + 1;
    if (!arch_big_endian) ByteSwapInPlace(&marker, 1);
    rv = fwrite(&marker, sizeof(unsigned short), 1, pFile);
    unsigned short exiflength = 0;
    // TODO save the correct length.
    int exif_len_pos = ftell(pFile);
    rv = fwrite(&exiflength, sizeof(unsigned short), 1, pFile);
    printf("Saving %lu Exif IFDs\n", ifds_.size());
    unsigned int exifmarker = 0x45786966;
    if (!arch_big_endian) ByteSwapInPlace(&exifmarker, 1);
    rv = fwrite(&exifmarker, sizeof(unsigned int), 1, pFile);
    unsigned short pad = 0;
    rv = fwrite(&pad, sizeof(unsigned short), 1, pFile);
    int subfileoffset = ftell(pFile);
    unsigned int forty_two = 0x002a;
    unsigned int byte_order = 0x4949;
    if (arch_big_endian) byte_order = 0x4d4d;
    rv = fwrite(&byte_order, sizeof(unsigned short), 1, pFile);
    rv = fwrite(&forty_two, sizeof(unsigned short), 1, pFile);
    unsigned int exifoffset_location = ftell(pFile);
    //    unsigned int subfile_offset = exifoffset_location;
    pending_pointers.push_back(exifoffset_location); // Where

    unsigned int exifoffset = 0; // Dummy
    rv = fwrite(&exifoffset, sizeof(unsigned int), 1, pFile);
    unsigned int zero = 0x00000000;
    for (int i = 0 ; i < ifds_.size(); ++i) {
      unsigned int ifdloc = ifds_[i]->Write(pFile, zero, subfileoffset);
      // "What":  Where we wrote this ifd (to be put in the previous "where")
      pending_pointers.push_back(ifdloc - subfileoffset);
      // "Where" we write the pointer to the next ifd.
      pending_pointers.push_back(ifdloc + 2 + 12*ifds_[i]->GetNTags());
    }
    pending_pointers.push_back(0);  // What: After last IFD we put 00000

    rv = fseek(pFile, 0, SEEK_END);
    // Finally fill in the unresolved pointers.
    exiflength = ftell(pFile) - exif_len_pos;
    fseek(pFile, exif_len_pos, SEEK_SET); // Where
    if (!arch_big_endian) ByteSwapInPlace(&exiflength, 1);
    rv = fwrite(&exiflength, sizeof(unsigned short), 1, pFile);
    for(int j = 0; j < pending_pointers.size(); j+=2) {
      printf("IFD Locs Where: %d What: %d\n",
	     pending_pointers[j], pending_pointers[j + 1]);
      fseek(pFile, pending_pointers[j], SEEK_SET); // Where
      unsigned int ifdloc = pending_pointers[j + 1];  // What
      // Pending pointers are written in native byte order.
      //      if (!arch_big_endian) ByteSwapInPlace(&ifdloc, 1);
      rv = fwrite(&ifdloc, sizeof(ifdloc), 1, pFile);
    }
    rv = fseek(pFile, 0, SEEK_END);
  }
  // Write the other markers.
  printf("Saving : %lu markers\n", markers_.size());
  for (int i = 0 ; i < markers_.size(); ++i) {
    if (markers_[i]->Save(pFile)==0)
      printf("Failed with marker %d\n", i);;
  }
  fclose(pFile);
}

// Having loaded a dht block into memory actually construct the DHTs.
void Jpeg::BuildDHTs(const JpegMarker *dht_block) {
  unsigned char *data = (unsigned char *)(&dht_block->data_[0]);
  int length = dht_block->length_ - 2;
  int bytes_used = 0;
  int table = 0;
  while (bytes_used < length) {
    JpegDHT *dht = new JpegDHT;
    int bytes = dht->Build(data + bytes_used, length-bytes_used);
    bytes_used += bytes;
    printf("DHT %d %d%d. Bytes=%d total = %d length = %d\n",
	   table, dht->class_, dht->id_, bytes, bytes_used, length);
    dhts_.push_back(dht);
    ++table;
  }
}

void Jpeg::ParseImage(const Redaction &redact, const char *pgmout) {
  JpegMarker *sos_block = GetMarker(jpeg_sos);
  unsigned char *data = (unsigned char *)(&sos_block->data_[0]);
  const int data_length = sos_block->length_ - 2;
  // First 2 bytes are slice, 00 0c 03 01 00 02 11 03 11 00 3f 00
  // then 03
  // then addl info 9 more bytes.
  // Then huffman bits.
  // const int check_offset = 0;
  // const int check_len = 64;
  data += 10;

  JpegDecoder decoder(width_, height_, data, data_length - 12,
                      dhts_, &components_);
  printf("\n\nDecoding %lu\n", sos_block->data_.size());
  //  DumpHex((unsigned char*)&sos_block->data_[check_offset], check_len);
  try {
    Redaction::Rect rect(50, 300, 50, 200);
    decoder.AddRedactionRegions(redact);
    decoder.Decode();
  } catch (const char *text) {
    printf("In Decoder: Caught error %s\n", text);
    //    throw(text);
  }
  decoder.ReorderImageData();
  if (pgmout != NULL)
    decoder.WriteImageData(pgmout);
  // Keep the 10 byte header.
  printf("Redacted data length %lu\n", decoder.redacted_data_.size());
  sos_block->data_.erase(sos_block->data_.begin() + 10, sos_block->data_.end());
  sos_block->data_.insert(sos_block->data_.end(),
			  decoder.redacted_data_.begin(),
			  decoder.redacted_data_.end());
  // Add the EOI
  sos_block->data_.push_back(0xff);
  sos_block->data_.push_back(0xd9);
  // Now keep on dumping data out.
  printf("H %d W %d\n", width_, height_);
  //  DumpHex((unsigned char*)&sos_block->data_[check_offset], check_len);
}

// Save this (preloaded) marker to disk.
int JpegMarker::Save(FILE *pFile) {
  unsigned short markerswapped = byteswap2(marker_);
  int rv = fwrite(&markerswapped, sizeof(unsigned short), 1, pFile);
  if (rv != 1) return 0;
  unsigned short slice_or_length = length_;
  if (marker_ == Jpeg::jpeg_sos)
    slice_or_length = slice_;
  slice_or_length = byteswap2(slice_or_length);
  rv = fwrite(&slice_or_length, sizeof(unsigned short), 1, pFile);
  if (rv != 1) return 0;
  rv = fwrite(&data_[0], sizeof(char), data_.size(), pFile);
  if (rv != data_.size()) return 0;
  printf("Saved marker %04x length %u\n", marker_, length_);
  return 1;
}
} // namespace redaction
