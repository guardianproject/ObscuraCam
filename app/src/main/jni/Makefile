#  Copyright (C) 2011 Andrew W. Senior andrew.senior[AT]gmail.com
#  Part of the Jpeg-Redaction-Library to read, parse, edit redact and
#  write JPEG/EXIF/JFIF images.
#  See https://github.com/asenior/Jpeg-Redaction-Library

#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.

#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.

#  You should have received a copy of the GNU General Public License
#  along with this program.  If not, see <http://www.gnu.org/licenses/>.


# makefile for building JPEG/EXIF tests under cygwin
include ../Makefile.common

LOCALLIB=libredact.a

default: lib

lib: $(LOCALLIB)

SRCS  =  debug_flag.cpp iptc.cpp jpeg.cpp  jpeg_decoder.cpp jpeg_marker.cpp \
        byte_swapping.cpp tiff_ifd.cpp tiff_tag.cpp

OBJS    = $(SRCS:.cpp=.o)

$(OBJS): $(SRCS:.cpp=.h)


$(LOCALLIB): $(OBJS)
	$(MKLIB)

.PHONY: clean lib test

test:
	cd ../test; $(MAKE) test

clean:
	rm -f $(OBJS) $(LOCALLIB)

depend: 
	$(MAKE) dependlocal

# DO NOT DELETE

