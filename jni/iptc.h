// iptc.h: interface for the Iptc class.
//
//////////////////////////////////////////////////////////////////////

#if !defined(INCLUDE_IPTC)
#define INCLUDE_IPTC

#include <stdio.h>
#include <vector>
#include "byte_swapping.h"

using std::vector;

// Parse an IPTC block from a file.
// e.g. within a photoshop 3 block
// See http://www.fileformat.info/format/psd/egff.htm

namespace jpeg_redaction {
class Iptc
{

public:
  class IptcTag
  {
  public:
  /*  typedef enum {
      iptc_format_unknown =0 ,
        iptc_format_binary = 1,
        iptc_format_byte = 2,
        iptc_format_short = 3,
        iptc_format_long = 4,
        iptc_format_string = 5,
        iptc_format_numeric_string = 6,
        iptc_format_date = 8,
        iptc_format_time =9
    } iptc_format; */


    typedef enum {
      iptc_tag_model_version		= 0,	/* begin record 1 tags */
        iptc_tag_destination		= 5,
        iptc_tag_file_format		= 20,
        iptc_tag_file_version		= 22,
        iptc_tag_service_id		= 30,
        iptc_tag_envelope_num		= 40,
        iptc_tag_product_id		= 50,
        iptc_tag_envelope_priority	= 60,
        iptc_tag_date_sent		= 70,
        iptc_tag_time_sent		= 80,
        iptc_tag_character_set		= 90,
        iptc_tag_uno			= 100,
        iptc_tag_arm_id			= 120,
        iptc_tag_arm_version		= 122,	/* end record 1 tags */
        iptc_tag_record_version		= 0,	/* begin record 2 tags */
        iptc_tag_object_type		= 3,
        iptc_tag_object_attribute	= 4,
        iptc_tag_object_name		= 5,
        iptc_tag_edit_status		= 7,
        iptc_tag_editorial_update	= 8,
        iptc_tag_urgency		= 10,
        iptc_tag_subject_reference	= 12,
        iptc_tag_category		= 15,
        iptc_tag_suppl_category		= 20,
        iptc_tag_fixture_id		= 22,
        iptc_tag_keywords		= 25,
        iptc_tag_content_loc_code	= 26,
        iptc_tag_content_loc_name	= 27,
        iptc_tag_release_date		= 30,
        iptc_tag_release_time		= 35,
        iptc_tag_expiration_date	= 37,
        iptc_tag_expiration_time	= 38,
        iptc_tag_special_instructions	= 40,
        iptc_tag_action_advised		= 42,
        iptc_tag_reference_service	= 45,
        iptc_tag_reference_date		= 47,
        iptc_tag_reference_number	= 50,
        iptc_tag_date_created		= 55,
        iptc_tag_time_created		= 60,
        iptc_tag_digital_creation_date	= 62,
        iptc_tag_digital_creation_time	= 63,
        iptc_tag_originating_program	= 65,
        iptc_tag_program_version	= 70,
        iptc_tag_object_cycle		= 75,
        iptc_tag_byline			= 80,
        iptc_tag_byline_title		= 85,
        iptc_tag_city			= 90,
        iptc_tag_sublocation		= 92,
        iptc_tag_state			= 95,
        iptc_tag_country_code		= 100,
        iptc_tag_country_name		= 101,
        iptc_tag_orig_trans_ref		= 103,
        iptc_tag_headline		= 105,
        iptc_tag_credit			= 110,
        iptc_tag_source			= 115,
        iptc_tag_copyright_notice	= 116,
        iptc_tag_picasa_unknown		= 117,
        iptc_tag_contact		= 118,
        iptc_tag_caption		= 120,
        iptc_tag_writer_editor		= 122,
        iptc_tag_rasterized_caption	= 125,
        iptc_tag_image_type		= 130,
        iptc_tag_image_orientation	= 131,
        iptc_tag_language_id		= 135,
        iptc_tag_audio_type		= 150,
        iptc_tag_audio_sampling_rate	= 151,
        iptc_tag_audio_sampling_res	= 152,
        iptc_tag_audio_duration		= 153,
        iptc_tag_audio_outcue		= 154,
        iptc_tag_preview_format		= 200,
        iptc_tag_preview_format_ver	= 201,
        iptc_tag_preview_data		= 202,	/* end record 2 tags */

        iptc_tag_size_mode		= 10,	/* begin record 7 tags */
        iptc_tag_max_subfile_size	= 20,
        iptc_tag_size_announced		= 90,
        iptc_tag_max_object_size	= 95,	/* end record 7 tags */

        iptc_tag_subfile		= 10,	/* record 8 tags */
        iptc_tag_confirmed_data_size	= 10	/* record 9 tags */
    } iptc_tag;

    IptcTag(FILE *pFile)
    {
      unsigned char datasetmarker;
      int iRV = fread(&datasetmarker, sizeof(unsigned char), 1, pFile);
      if (iRV !=1 || datasetmarker != Iptc::tag_marker_)
	throw("Got a bad tag marker");

      iRV = fread(&record_, sizeof(unsigned char), 1, pFile);
      if (iRV != 1) throw("IPTC read fail");

      iRV = fread(&tag_, sizeof(unsigned char), 1, pFile);
      if (iRV != 1) throw("IPTC read fail");

      unsigned long length = 0;
      unsigned short shortlength;
      iRV = fread(&shortlength, sizeof(unsigned short), 1, pFile);
      if (iRV != 1) throw("IPTC read fail");
      shortlength = byteswap2(shortlength);

      if (shortlength & 0x8000) { // Top bit is set: p14 of IPTC IIMV
        shortlength &= 0x7ffff;  // Number of bytes in which the actual length is stored.
        if (shortlength > 4) throw("Can't handle payloads longer than 2^32 bytes");
        while (shortlength > 0) {
          unsigned char b;
          iRV = fread(&b, sizeof(unsigned char), 1, pFile);
          length += (b << (8*shortlength));  // TODO Maybe wrong-endian??
          --shortlength;
        }
      } else {
        length = shortlength;
      }

      data_.resize(length);
      iRV = fread(&data_[0], sizeof(unsigned char), length, pFile);

      if (iRV != length) throw("IPTC read fail length");
      printf("IPTC dataset %d,%d\n", record_, tag_);
    }


    virtual ~IptcTag() {  }


    // Size of the tag, including the marker, record no, tag & data.
    int DataLength() const {
      return data_.size() + 5;
    }

    int Write(FILE *pFile)
    {
      int iRV = fwrite(&Iptc::tag_marker_, sizeof(unsigned char), 1, pFile);
      if (iRV != 1) throw("IPTC write fail");
      iRV = fwrite(&record_, sizeof(unsigned char), 1, pFile);
      if (iRV != 1) throw("IPTC write fail");

      iRV = fwrite(&tag_, sizeof(unsigned char), 1, pFile);
      if (iRV != 1) throw("IPTC write fail");

      unsigned short length = data_.size();
      iRV = fwrite(&length, sizeof(unsigned short), 1, pFile);
      if (iRV != 1) throw("IPTC write fail");

      iRV = fwrite(&data_[0], sizeof(unsigned char), length, pFile);

      if (iRV != length) throw("IPTC write fail length");

      return 5 + length;
    }

    unsigned char tag_;
    std::vector<unsigned char> data_;
    unsigned char record_;
}; // End of IptcTag


// Read an IPTC block in from a file.
Iptc(FILE *pFile, unsigned int totallength)
{
  unsigned char bindummy;
  const int mintaglength = 5;
  int remaininglength = totallength;
  int iRV;
  while (remaininglength >= mintaglength) {
    IptcTag *tag= new IptcTag(pFile);
    if (tag == NULL)
      throw("Got null IPTC tag");
    int thistaglength = tag->DataLength();
    tags_.push_back(tag);
    if (thistaglength > remaininglength) throw("IPTC tag too long");
    remaininglength -= thistaglength;
  }
  if (remaininglength != 0)
    throw("IPTC block length error");

  if (totallength %2 == 1) // Length is rounded to be even.
    iRV = fread(&bindummy, sizeof(unsigned char), 1, pFile);
}

virtual ~Iptc()
{
  for(int i = 0; i< tags_.size(); ++i) {
    delete tags_[i];
  }
  tags_.clear();
}

int Write(FILE *pFile)
{
  int length = 0;
  for (int i = 0; i < tags_.size(); ++i) {
    int iRV = tags_[i]->Write(pFile);
    length += iRV;
  }
  return length;
}
static const unsigned char tag_marker_;
static const unsigned int tag_bim_;
static const unsigned short tag_bim_iptc_;

std::vector<IptcTag *> tags_;
};
}  // namespace jpeg_redaction

#endif // INCLUDE_IPTC
