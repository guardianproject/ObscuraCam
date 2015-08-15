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


// obscura_metadata.h

// A class to store metadata specific to ObscuraCam in APPn (n=7) blocks
// of JPEG files.
// A general metadata block to be parsed by the application.
// and a redaction-reversal block to be parsed by this library.

#ifndef INCLUDE_OBSCURA_METADATA
#define INCLUDE_OBSCURA_METADATA

#include <vector>
#include "jpeg.h"  // For jpeg_app marker.
#include "debug_flag.h"
#include "jpeg_marker.h"
#define JPEG_APP0 0xFFE0
namespace jpeg_redaction {

class ObscuraMetadata {
public:
  ObscuraMetadata() {}
  void SetDescriptor(unsigned int length,
		     const unsigned char *data) {
    descriptor_.resize(length);
    if (length > 0)
      memcpy(&descriptor_[0], data, length);
    printf("Set Obscura Metadata length %zu\n", descriptor_.size());
  }
  const unsigned char *GetDescriptor(unsigned int *length) const {
    if (descriptor_.size() > 0) {
      *length = descriptor_.size();
      return &descriptor_[0];
    }
    *length = 0;
    return NULL;
  }
  
  // Return true if the marker was understood and stored in this structure.
  bool ImportMarker(JpegMarker * marker) {
    if (marker == NULL || marker->marker_ != kObscuraMarker) {
      printf("Got a marker not obscura but %x\n", marker->marker_);
      return false;
    }
    if (strcmp((char*)&marker->data_.front(), kDescriptorType) == 0) {
      if (debug > 0) {
	printf("Reading Obscura metadata \"%s\" %d %zu - %zu\n",
	       kDescriptorType, marker->length_,
	       marker->data_.size(), strlen(kDescriptorType));
	marker->Print();
      }
      int marker_no;
      int num_markers;
      int marker_len;
      int marker_start;
      int descriptor_len;
      unsigned char * marker_ptr =
	&(marker->data_.front()) + strlen(kDescriptorType) + 1;
      memcpy(&marker_no, marker_ptr, sizeof(int));
      marker_ptr += sizeof(int);
      memcpy(&num_markers, marker_ptr, sizeof(int));
      marker_ptr += sizeof(int);
      memcpy(&marker_len, marker_ptr, sizeof(int));
      marker_ptr += sizeof(int);
      memcpy(&marker_start, marker_ptr, sizeof(int));
      marker_ptr += sizeof(int);
      memcpy(&descriptor_len, marker_ptr, sizeof(int));
      marker_ptr += sizeof(int);
      if (debug > 0) {
	printf("Read Obscura marker %d/%d len %d at %d of %d\n", 
	       marker_no, num_markers, 
	       marker_len, marker_start, descriptor_len);
      }
      if (marker_no <0 || num_markers <=0 || marker_no >= num_markers)
	throw("marker numbers don't match");
      if (marker_len < 0 || descriptor_len <=0 || marker_no >= num_markers ||
	  marker_len >= 64 * 1024 ||
	  marker_start + marker_len > descriptor_len ||
	  marker_start < 0)
	throw("marker lengths don't match");
      if (marker_no == 0)
	descriptor_.resize(descriptor_len);
      else
	if (descriptor_.size() != descriptor_len)
	  throw("Marker store size != descriptor_len");
      memcpy(&descriptor_[marker_start], marker_ptr, marker_len);
      if (marker_no == num_markers -1) {
	if (marker_start + marker_len != descriptor_len) {
	  printf("Marker length @ %d /%d  inexact %d %d %d\n", 
		 marker_no, num_markers, 
		 marker_start, marker_len, descriptor_len);
	  throw("marker length inexact");
	}
      // SetDescriptor(marker->length_ - 2 - (strlen(kDescriptorType) + 1),
      // 		    &marker->data_[strlen(kDescriptorType) + 1]);
      }
      return true;
    }
    fprintf(stderr, "Unknown AppN Marker %d: %s\n",
	    marker->GetBitLength(), (char*)&marker->data_.front());
    return false;
  }
  // Store all the metadata in a file as APPN JPEG Markers.
  int Write(FILE *pFile) {
    // Convert the data to markers and write.
    std::vector<JpegMarker *> descriptors;
    MakeDescriptorMarkers(&descriptors);
    if (descriptors.size() != 0) {
      if (debug > 0)
	printf("Writing Obscura descriptor at %zu\n",
	       ftell(pFile));
      for (int i=0; i < descriptors.size(); ++i) {
	descriptors[i]->Save(pFile);
	delete descriptors[i];
      }
    }
    return 0;
  }

  static const int kObscuraMarker;
  static const char *kDescriptorType;
protected:
  // Make a JPEG marker containing the descriptor information.
   void MakeDescriptorMarkers(std::vector<JpegMarker *> *all_markers) {
    if (descriptor_.size() == 0)
      return;
    const int header_len = strlen(kDescriptorType) + 1 + 5 * sizeof(int);
    const int marker_size = 64 * 1024 - header_len - 4;
    const int des_length = descriptor_.size();
    const int num_markers = (des_length + marker_size - 1) / marker_size;
    int marker_start = 0;
    for (int count = 0; count < num_markers; ++count) {
      int marker_len = marker_size;
      if (des_length < marker_start + marker_len)
	marker_len = des_length - marker_start;
      const int total_length = marker_len + header_len;
    // Create a temporary buffer to store the string header and the
    // marker data.
      std::vector<unsigned char> long_data(total_length);
      unsigned char *data_ptr = &long_data[0];
      memcpy(data_ptr, kDescriptorType, strlen(kDescriptorType)+1);
      data_ptr += strlen(kDescriptorType)+1;
      memcpy(data_ptr, &count, sizeof(int));
      data_ptr += sizeof(int);
      memcpy(data_ptr, &num_markers, sizeof(int));
      data_ptr += sizeof(int);
      memcpy(data_ptr, &marker_len, sizeof(int));
      data_ptr += sizeof(int);
      memcpy(data_ptr, &marker_start, sizeof(int));
      data_ptr += sizeof(int);
      memcpy(data_ptr, &des_length, sizeof(int));
      data_ptr += sizeof(int);
      memcpy(data_ptr, &descriptor_[marker_start], marker_len);
      data_ptr += marker_len;
      if (debug > 0) {
	printf("Making descriptor %d/%d len  %d at %d of %d\n",
	       count, num_markers, marker_len, marker_start, des_length);
      }
      if (data_ptr - &long_data[0] != total_length)
	throw("Length mismatch making markers");
      JpegMarker *marker = new JpegMarker(kObscuraMarker, &long_data.front(),
					  total_length);
      marker->Print();
      all_markers->push_back(marker);
      marker_start += marker_len;
    }
    if (marker_start != des_length)
      throw("ObscuraMarker lenght mismatch after marker creation.");
  }
  std::vector<unsigned char> descriptor_;
};
}  // namespace redaction

#endif
