
# for fink install: glib2-dev gstreamer-0.10-dev libxml2 gst-plugins-base-0.10 gst-plugins-good-0.10 gst-plugins-ugly-0.10 gst-plugins-bad-0.10 gst-plugins-0.10

SOURCES = gstreamer_helloworld.c
EXECUTABLE = gstreamer_helloworld

CFLAGS := $(shell pkg-config --cflags gstreamer-0.10 glib-2.0)
LDFLAGS := $(shell pkg-config --libs gstreamer-0.10 glib-2.0)
#LIBS := $(shell pkg-config --libs gstreamer-0.10 glib-2.0)

all: $(EXECUTABLE)

%.o: %.c
	$(CC) $(CFLAGS) -o $*.o -c $*.c

$(EXECUTABLE): $(SOURCES:.c=.o)
	$(CC) $(LDFLAGS) -o $(EXECUTABLE) $(SOURCES:.c=.o)


clean:
	rm -f -- $(SOURCES:.c=.o)
	rm -f -- $(EXECUTABLE)
