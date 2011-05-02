# makefile for building JPEG/EXIF tests under cygwin
include ../Makefile.common

LOCALLIB=libredact.a

lib: $(LOCALLIB)

SRCS  =  iptc.cpp jpeg.cpp  tiff_ifd.cpp tiff_tag.cpp jpeg_decoder.cpp \
        byte_swapping.cpp

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
