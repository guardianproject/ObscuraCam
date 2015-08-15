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

// jpeg.cpp: implementation of the Jpeg class to store all the information
// from a JPEG file.

#include "debug_flag.h"
#include "jpeg.h"
#include "jpeg_dht.h"
#include "jpeg_decoder.h"
#include "jpeg_marker.h"
#include "redaction.h"
#include "photoshop_3block.h"
#include "byte_swapping.h"
#include "obscura_metadata.h"

//////////////////////////////////////////////////////////////////////
// Construction/Destruction
//////////////////////////////////////////////////////////////////////

namespace jpeg_redaction {
 const int ObscuraMetadata::kObscuraMarker = JPEG_APP0 + 7;
 const char *ObscuraMetadata::kDescriptorType = "ObscuraMetaData";

  void DumpHex(unsigned char *data, int len) {
    for (int i = 0; i < len; ++i) {
      printf("%02x ", data[i]);
      if ((i +1) %16 == 0)
	printf("\n");
    }
    if (len %16 != 0)
      printf("\n");
  }
  bool Jpeg::LoadFromFile(char const * const pczFilename, bool loadall) {
    filename_ = pczFilename;
    //  exif_ = NULL;

    FILE *pFile = fopen(pczFilename, "rb");
    if (pFile == NULL) {
      fprintf(stderr, "Couldn't open file %s\n", pczFilename);
      return false;
    }
    int rv = LoadFromFile(pFile, loadall, 0);
    fclose(pFile);
    return rv == 0;
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
    const bool arch_big_endian = ArchBigEndian();
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
      if (!arch_big_endian)
	ByteSwapInPlace(&marker, 1);
      if (debug > 0)
	printf("Got marker 0x%x %s at %d\n",
	       marker, MarkerName(marker), blockloc);

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
	unsigned short blocksize;
	unsigned int magic = 0, exifoffset = 0;
	// unsigned short myshort;
	iRV = fread(&blocksize, sizeof(unsigned short), 1, pFile);
	if (!arch_big_endian)
	  ByteSwapInPlace(&blocksize, 1);
	if (debug > 0)
	  printf("Photoshop 0x%x at %zu length %d\n",
		 marker, ftell(pFile), blocksize);
	try {
	  photoshop3_ = new Photoshop3Block(pFile, blocksize-sizeof(blocksize));
	}  catch (char *ex) {
	  fprintf(stderr, "Got exception %s", ex);
	  throw(ex);
	}
	fseek(pFile, blockloc + blocksize + sizeof(marker), SEEK_SET);
	continue;
      }
      // If it's not an app_n marker we've already dealt with above.
      if (marker >= jpeg_app && marker < jpeg_app + 16) {
	unsigned short blocksize;
	iRV = fread(&blocksize, sizeof(unsigned short), 1, pFile);
	if (!arch_big_endian)
	  ByteSwapInPlace(&blocksize, 1);

	JpegMarker *newmarker = AddMarker(marker, blockloc, blocksize,
					  pFile, loadall);
	if (newmarker != NULL) {
	  char *cp = (char *)&(newmarker->data_.front());
	  int i = 0;
	  const int max_string_length = 12;
	  for (; i < max_string_length && i < newmarker->data_.size(); ++i) {
	    if (cp[i] == '\0') {
	      printf("Generic APPn %x loaded. Marker string: %s\n",
		     marker, cp);
	      break;
	    }
	  }
	  if (i >= max_string_length)
	    printf("Generic APPn %x loaded. Non-string marker.\n", marker);
	  }
	continue;
      }
      // http://www.bsdg.org/swag/GRAPHICS/0143.PAS.html
      if (marker == jpeg_sof0 || marker == jpeg_sof2) {
	unsigned short blocksize;
	iRV = fread(&blocksize, sizeof(unsigned short), 1, pFile);
	if (!arch_big_endian)
	  ByteSwapInPlace(&blocksize, 1);
	if (debug > 1)
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
	  fprintf(stderr, "Error - wrong blocksize ins SOF\n");
	}
	for (int i = 0; i < components; ++i) {
	  JpegComponent *comp = new JpegComponent(data + 6 + 3 * i);
	  comp->Print();
	  components_.push_back(comp);
	}
	if (debug > 0)
	  printf("JPEG Image is %d x %d\n", width_, height_);
	continue;
      }
      if (marker == jpeg_dqt || marker == jpeg_dht) {
	unsigned short blocksize;
	iRV = fread(&blocksize, sizeof(unsigned short), 1, pFile);
	if (!arch_big_endian)
	  ByteSwapInPlace(&blocksize, 1);
	if (debug > 0)
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
	if (!arch_big_endian)
	  ByteSwapInPlace(&blocksize, 1);
	JpegMarker *dri = AddMarker(marker, blockloc, blocksize, pFile, true);
	restartinterval_ = *(short*)(&dri->data_[0]);
	if (!arch_big_endian)
	  ByteSwapInPlace(&restartinterval_, 1);
	if (debug > 1)
	  printf("Restart interval %d\n", restartinterval_);
	continue;
      }
      if (marker == jpeg_sos) { // Start of scan
	return ReadSOSMarker(pFile, blockloc, loadall);
      }
      fprintf(stderr, "Unknown marker is 0x%04x.\n", marker);
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
      ByteSwapInPlace(&blocksize, 1);
    if (debug > 0)
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
    if (debug > 0)
      printf("EXIF byte_order: %04x\n", byte_order);
    if (byte_order == 0x4d4d) // "Motorola"
      big_endian = true;
    else if (byte_order != 0x4949) // "Intel"
      throw("Don't recognize EXIF tag");
    if (debug > 0)
      printf("EXIF block is: %s endian (%d), arch is: %s (%d)\n",
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
    if (debug > 0)
      printf("Exifloc %d/0x%x Offset %d/0x%x\n",
	     exifloc, exifloc, exifoffset, exifoffset);
    // Absolute location of IFD:
    unsigned int ifdoffset = exifloc;
    while (ifdoffset > 0) {
      const unsigned int subfileoffset = blockloc + 10;
      if (debug > 0)
	printf("Loading IFD %lu @%u subfileoffset %u swap %d ",
	       ifds_.size(), ifdoffset, subfileoffset, byte_swapping);
      TiffIfd *tempifd = new TiffIfd(pFile, ifdoffset,
				     loadall, subfileoffset, byte_swapping);
      ifds_.push_back(tempifd);
      ifdoffset = tempifd->GetNextIfdOffset();
      if (ifdoffset != 0)
	ifdoffset += subfileoffset;
      if (debug > 0)
	printf("Next offset %d\n", ifdoffset);
    }
    //      exif_ = new TiffIfd(pFile, exifloc, true, blockloc + 10); // Need to pass the baseline.
    iRV = fseek(pFile, blockloc + blocksize + sizeof(unsigned short),
		SEEK_SET);
    return 0;
  }

  int Jpeg::ReadSOSMarker(FILE *pFile, unsigned int blockloc, bool loadall) {
    const bool arch_big_endian = ArchBigEndian();
    short slice = 0;
    int iRV = fread(&slice, sizeof(unsigned short), 1, pFile);
    if (!arch_big_endian) ByteSwapInPlace(&slice, 1);
    if (debug > 0)
      printf("SOS slice %d\n", slice);
    int dataloc = blockloc + sizeof(unsigned short); // marker's size
    unsigned int buf = 0;
    int datalen = 0;

    while (1) { // Read the data looking for markers
      buf <<= 8;
      iRV = fread(&buf, sizeof(unsigned char), 1, pFile);
      if (iRV != 1) {
	fprintf(stderr,
		"ReadSOSMarker: Failed to load byte at %d (datalen %d)\n",
		blockloc + 4 + datalen, datalen);
	throw(-10);
      }
      datalen++;
      if (debug > 0 && (buf & 0xff00) == 0xff00 && (buf & 0xffff)!= 0xff00)
      	printf("In scan found marker 0x%x\n", (buf & 0xffff));
      if ((buf & 0xffff) == jpeg_eoi) {
	if (debug > 0)
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

  // Save the image to disk (after preloading).
  int Jpeg::Save(const char * const filename) {
    const bool arch_big_endian = ArchBigEndian();
    FILE *pFile = fopen(filename, "wb");
    if (pFile == NULL) {
      fprintf(stderr, "Can't open output file : %s\n", filename);
      return 1;
    }
    int rv = Save(pFile);
    fclose(pFile);
    return rv;
  }

  // Save the image to disk (after preloading).
  int Jpeg::Save(FILE *pFile) {
    const bool arch_big_endian = ArchBigEndian();
    if (pFile == NULL) {
      throw("NULL file in Jpeg::Save");
    }
    // Write the header,
    unsigned short magic = 0xd8ff;
    if (arch_big_endian) ByteSwapInPlace(&magic, 1);
    int rv = fwrite(&magic, sizeof(unsigned short), 1, pFile);
    bool write_exif = true;

    // write the EXIF IFDs  Code based on CR2.cpp
    if (write_exif && ifds_.size() !=0) {
      if (debug > 0)
	printf("Writing exif at %zu\n", ftell(pFile));
      std::vector<unsigned int> pending_pointers;  // Pairs of Where/What
      unsigned short marker = jpeg_app + 1;
      if (!arch_big_endian) ByteSwapInPlace(&marker, 1);
      rv = fwrite(&marker, sizeof(unsigned short), 1, pFile);
      unsigned short exiflength = 0;
      // TODO save the correct length.
      int exif_len_pos = ftell(pFile);
      rv = fwrite(&exiflength, sizeof(unsigned short), 1, pFile);
      if (debug > 0)
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
	if (debug > 0)
	  printf("Writing IFD %d at %zu\n", i, ftell(pFile));

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
	if (debug > 0)
	  printf("IFD Locs Where: %d What: %d\n",
		 pending_pointers[j], pending_pointers[j + 1]);
	fseek(pFile, pending_pointers[j], SEEK_SET); // Where
	unsigned int ifdloc = pending_pointers[j + 1];  // What
	// Pending pointers are written in native byte order.
	//      if (!arch_big_endian) ByteSwapInPlace(&ifdloc, 1);
	rv = fwrite(&ifdloc, sizeof(ifdloc), 1, pFile);
      }
      rv = fseek(pFile, 0, SEEK_END);
    }  // Write exif IFDs
    obscura_metadata_.Write(pFile);
    if (photoshop3_) {
      unsigned short marker =jpeg_app + 0xd;
      if (!arch_big_endian)
	ByteSwapInPlace(&marker, 1);
      int rv = fwrite(&marker, sizeof(unsigned short), 1, pFile);
      unsigned short blocksize = 0;
      unsigned int blockloc = ftell(pFile);
      rv = fwrite(&blocksize, sizeof(unsigned short), 1, pFile);
      blocksize = photoshop3_->Write(pFile) + sizeof(blocksize);
      if (debug > 0)
	printf("  IPTC Written bytes: %zu vs %d\n", ftell(pFile) - blockloc,
	       blocksize);
      fseek(pFile, blockloc, SEEK_SET);
      if (!arch_big_endian)
	ByteSwapInPlace(&blocksize, 1);
      rv = fwrite(&blocksize, sizeof(unsigned short), 1, pFile);
      fseek(pFile, 0, SEEK_END);
    }
    // Write the other markers.
    if (debug > 0)
      printf("Saving: %lu markers\n", markers_.size());
    for (int i = 0 ; i < markers_.size(); ++i) {
      if (markers_[i]->Save(pFile)==0)
	fprintf(stderr, "Failed with marker %d\n", i);;
    }
    return 0;
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
      if (debug > 0)
	printf("DHT %d %d%d. Bytes=%d total = %d length = %d\n",
	       table, dht->class_, dht->id_, bytes, bytes_used, length);
      dhts_.push_back(dht);
      ++table;
    }
  }
  // First find the IFD with tags 0x201 & 0x202.
  // should contain data_
  Jpeg *Jpeg::GetThumbnail() {
    for (int i = 0 ; i < ifds_.size(); ++i) {
      if (ifds_[i]->GetJpeg() &&
	  ifds_[i]->FindTag(TiffTag::tag_ThumbnailOffset))
	return ifds_[i]->GetJpeg();
    }
    return NULL;
  }

  // Redact the thumbnail 
  // Returns: 0: no thumbnail found N:  N thumbnails found and redacted
  // <0 failed to redact thumbnail.
  int Jpeg::RedactThumbnail(Redaction *redaction) {
    if (debug > 0)
      printf("Redacting thumbnail\n");
    int return_val = 0;
    Jpeg *thumbnail = GetThumbnail();
    if (thumbnail == NULL)
      return 0;
    
    // Get thumbnail width/height
    const int width = thumbnail->GetWidth();
    const int height = thumbnail->GetHeight();
    // scale the redaction object
    Redaction *thumbnail_redaction = redaction->Copy();
    thumbnail_redaction->Scale(width, height, GetWidth(), GetHeight());
    // Call redaction to redact the SOS block.
    thumbnail->DecodeImage(thumbnail_redaction, NULL);
    delete thumbnail_redaction;
  }

  // Parse the JPEG image stream, applying redaction if provided.
  void Jpeg::DecodeImage(Redaction *redaction,
			 const char *pgm_save_filename) {
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

    JpegDecoder decoder(width_, height_, data, 8 * (data_length - 10),
			dhts_, &components_);
    if (debug > 0)
      printf("\n\nDecoding %lu\n", sos_block->data_.size());
    //  DumpHex((unsigned char*)&sos_block->data_[check_offset], check_len);
    try {
      decoder.Decode(redaction);
    } catch (const char *text) {
      fprintf(stderr, "In Decoder: Caught error %s\n", text);
      //    throw(text);
    }
    decoder.ReorderImageData();
    if (pgm_save_filename != NULL) {
      int rv = decoder.WriteImageData(pgm_save_filename);
      if (rv != 0)
	fprintf(stderr, "Couldn't write the decoded grey image to %s\n",
		pgm_save_filename);
    }
    if (redaction && redaction->NumRegions() > 0) {
      // Keep the 10 byte header.
      const std::vector<unsigned char> &redacted_data =
	decoder.GetRedactedData();
      if (debug > 0)
	printf("Redacted data length %lu bytes %d bits\n",
	       redacted_data.size(),
	       decoder.GetBitLength());
      sos_block->data_.erase(sos_block->data_.begin() + 10,
			     sos_block->data_.end());
      sos_block->data_.insert(sos_block->data_.end(),
			      redacted_data.begin(),
			      redacted_data.end());
      sos_block->SetBitLength(decoder.GetBitLength() + 10 * 8);
      if (debug > 0)
	printf("sos block now %zu bytes\n", sos_block->data_.size());
    }
    // Now keep on dumping data out.
    if (debug > 0)
      printf("DecodeImage H %d W %d\n", width_, height_);
    //  DumpHex((unsigned char*)&sos_block->data_[check_offset], check_len);
    if (redaction) RedactThumbnail(redaction);
  }

  int Jpeg::ReverseRedaction(const Redaction &redaction) {
    JpegMarker *sos_block = GetMarker(jpeg_sos);
    // For each strip insert it into the JPEG data.
    int data_bits = sos_block->GetBitLength();
    if (debug > 0)
      printf("Before patching size %zu bytes %d bits.\n",
	     sos_block->data_.size(), data_bits);
    // We pass the data with the header in it, so start at this bit.
    int offset = 10 * 8;
    for (int i = 0; i < redaction.NumStrips(); ++i) {
      if (debug > 0)
	printf("Patching in strip %d\n", i);
      // If we patch all the strips in, they need no offset.
      int shift = redaction.GetStrip(i)->PatchIn(offset, &sos_block->data_,
						 &data_bits);
      sos_block->SetBitLength(data_bits);
      //      offset += shift;
    }
  }
  int Jpeg::RemoveIPTC() {
    if (photoshop3_  != NULL) {
      delete photoshop3_;
      photoshop3_ = NULL;
      return 1;
    }
    return 0;
  }
  JpegMarker *Jpeg::GetMarker(int marker) {
    for (int i = 0 ; i < markers_.size(); ++i) {
      /* printf("Marker %d is %04x seeking %04x\n", */
      /* 	     i, markers_[i]->marker_, marker); */
      if (markers_[i]->marker_ == marker)
	return markers_[i];
    }
    return NULL;
  }

  JpegMarker *Jpeg::AddSOMarker(int location, int length,
				FILE *pFile, bool loadall, int slice) {
    // We have true byte length here, + 2 bytes of EOI. AddMarker needs
    // payload length which assumes there were 2 bytes of length.
    JpegMarker *markerptr = AddMarker(jpeg_sos, location, length + 2 - 2,
				      pFile, loadall);
    if (loadall)
      markerptr->RemoveStuffBytes();
    int rv = fseek(pFile, 2, SEEK_CUR);
    if (rv != 0)
      throw("Fail seeking in AddSOMarker");
    markerptr->slice_ = slice;
    return markerptr;
  }
  // The length is the length from the file, including the storage for length.
  JpegMarker *Jpeg::AddMarker(int marker, int location, int length,
			      FILE *pFile, bool loadall) {
    if (debug > 1)
      printf("Adding Marker %x\n", marker);
    JpegMarker *markerptr = new JpegMarker(marker, location, length);
    if (loadall)
      markerptr->LoadHere(pFile);
    else
      fseek(pFile, location + length + 2, SEEK_SET);
    if (marker == ObscuraMetadata::kObscuraMarker) {
      printf("Got obscura marker\n");
      obscura_metadata_.ImportMarker(markerptr);
      delete markerptr;
      return NULL;
    }
    markers_.push_back(markerptr);
    return markerptr;
  }
} // namespace jpeg_redaction
