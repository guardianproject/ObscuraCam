// jpeg.h: interface for the Jpeg class.
//
//////////////////////////////////////////////////////////////////////

#ifndef INCLUDE_JPEG
#define INCLUDE_JPEG

#include <vector>
#include <string>
#include <stdlib.h>
#include <stdio.h>
#include "tiff_ifd.h"

namespace jpeg_redaction {
extern int debug;

class Iptc;
class JpegDHT;
class Redaction;
class JpegMarker {
 public:
  // Length is payload size- includes storage for length itself.
  JpegMarker(unsigned short marker, unsigned int location,
	     int length) {
    marker_ = marker;
    location_ = location_;
    length_ = length;
  }
  void LoadFromLocation(FILE *pFile) {
    fseek(pFile, location_ + 4, SEEK_SET);
    LoadHere(pFile);
  }
  void LoadHere(FILE *pFile) {
    data_.resize(length_-2);
    int rv = fread(&data_[0], sizeof(char), length_-2, pFile);
    if (rv  != length_-2) {
      printf("Failed to read marker %x at %d\n", marker_, length_);
      throw("Failed to read marker");
    }
  }
  void WriteWithStuffBytes(FILE *pFile);
  void RemoveStuffBytes();
  int Save(FILE *pFile);
  unsigned short slice_;
  // Length is payload size- includes storage for length itself.
  int length_;
  unsigned int location_;
  unsigned short marker_;
  std::vector<unsigned char> data_;
};  // JpegMarker

class Photoshop3Block;
class Jpeg {
public:
  enum markers { jpeg_soi = 0xFFD8,
		 jpeg_sof0 = 0xFFC0,
		 jpeg_sof2 = 0xFFC2,
		 jpeg_dht = 0xFFC4,
		 jpeg_eoi = 0xFFD9,
		 jpeg_sos = 0xFFDA,
		 jpeg_dqt = 0xFFDB,
		 jpeg_dri = 0xFFDD,
		 jpeg_com = 0xFFDE,
		 jpeg_app = 0xFFE0};
  class JpegComponent {
  public:
    JpegComponent(unsigned char *d) {
      id_ = d[0];
      v_factor_ = d[1] & 0xf;
      h_factor_ = d[1] >> 4;
      table_ = d[2];
    }
    void Print() {
      printf("Component %d H %d V %d, Table %d\n",
	     id_, h_factor_, v_factor_, table_);
    }
    int id_;
    int h_factor_;
    int v_factor_;
    int table_;
  };
  // Trivial constructor.
  Jpeg() : filename_(""), width_(0), height_(0), photoshop3_(NULL) {};
  virtual ~Jpeg();
  // Construct from a file. loadall indicates whether to load all
  // data blocks, or just parse the file and extract metadata.
  bool LoadFromFile(char const * const pczFilename, bool loadall);
  int LoadFromFile(FILE *pFile, bool loadall, int offset);

  // Parse the JPEG data and redact if Redaction regions are supplied.
  // If pgm_save_filename is provided, write the decoded image to that file.

  void DecodeImage(Redaction *redaction, const char *pgm_save_filename);
  // Invert the redaction by pasting in the strips from redaction.
  int ReverseRedaction(const Redaction &redaction);

  // Save the current (possibly redacted) version of the JPEG out.
  // Return 0 on success.
  int Save(const char * const filename);

  const char *MarkerName(int marker) const;
  Iptc *GetIptc();
  // Return a pointer to the Exif IFD if present. 
  ExifIfd *GetExif() {
    for (int i=0; i < ifds_.size(); ++i)
      if (ifds_[i]->GetExif())
	return ifds_[i]->GetExif();
    return NULL;
  }
  int RemoveTag(int tag) {
    int removed = 0;
    for (int i=0; i < ifds_.size(); ++i) {
      bool removed_this = ifds_[i]->RemoveTag(tag);
      if (removed_this) ++removed;
    }
    return removed;
  }
  // Add here a list of all the tags that are considered sensitive.
  int RemoveAllSensitive() {
    RemoveTag(TiffTag::tag_exif);
    RemoveTag(TiffTag::tag_gps);
    RemoveTag(TiffTag::tag_makernote);
    RemoveTag(TiffTag::tag_make);
    RemoveTag(TiffTag::tag_model);
    // e.g. times, owner, IPTC etc.
  }

  JpegMarker *GetMarker(int marker) {
    for (int i = 0 ; i < markers_.size(); ++i) {
      /* printf("Marker %d is %04x seeking %04x\n", */
      /* 	     i, markers_[i]->marker_, marker); */
      if (markers_[i]->marker_ == marker)
	return markers_[i];
    }
    return NULL;
  }
  // If it's a SOF or SOS we pass a slice.
  JpegMarker *AddSOMarker(int location, int length,
			  FILE *pFile, bool loadall, int slice) {
    // We have true byte length here, + 2 bytes of EOI. AddMarker needs
    // payload length which assumes there were 2 bytes of length.
    JpegMarker *markerptr = AddMarker(jpeg_sos, location, length + 2 - 2,
				      pFile, loadall);
    if (loadall)
      markerptr->RemoveStuffBytes();
    fseek(pFile, 2, SEEK_CUR);
    markerptr->slice_ = slice;
    return markerptr;
  }
  // The length is the length from the file, including the storage for length.
  JpegMarker *AddMarker(int marker, int location, int length,
                        FILE *pFile, bool loadall) {
    JpegMarker *markerptr = new JpegMarker(marker, location, length);
    if (loadall)
      markerptr->LoadHere(pFile);
    else
      fseek(pFile, location + length + 2, SEEK_SET);
    markers_.push_back(markerptr);
    return markerptr;
  }
protected:
  // After loading an SO Marker, remove the stuff bytes so the bitstream
  // can be read more easily.
  void RemoveStuffBytes();
  void BuildDHTs(const JpegMarker *dht_block);
  int ReadSOSMarker(FILE *pFile, unsigned int blockloc, bool loadall);
  int LoadExif(FILE *pFile, unsigned int blockloc, bool loadall);

  std::vector<JpegMarker*> markers_;
  int width_;
  int height_;
  int softype_;
  std::vector<TiffIfd *> ifds_;
  std::string filename_;
  //  unsigned int datalen_, dataloc_;

  unsigned int restartinterval_;

  Photoshop3Block *photoshop3_;
  std::vector<JpegDHT*> dhts_;
  std::vector<JpegComponent*> components_;
};  // Jpeg
}  // namespace redaction

#endif // INCLUDE_JPEG
