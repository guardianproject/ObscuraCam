package org.witness.informa.utils;

public class InformaConstants {
	public final static String TAG = "************ INFORMA ***********";
	public final static int FROM_INFORMA_WIZARD = 3;

	public final static class INFORMA_SERVICE {
		public final static String STOP_SERVICE = "stopService";
		public final static String SET_CURRENT = "setCurrent";
		public final static String SEAL_LOG = "sealLog";
	}

	public final static class INFORMA_KEYS {
		public final static class CAPTURE_EVENTS {
			public final static int MediaCaptured = 5;
			public final static int MediaSaved = 6;
			public final static int RegionGenerated = 7;
			public final static int ExifReported = 8;
		}

		public final static class LOCATION_TYPES {
			public final static int LocationOnMediaCaptured = 9;
			public final static int LocationOnMediaSaved = 10;
			public final static int LocationOnGeneration = 11;
		}

		public final static class SECURITY_LEVELS {
			public final static int UnencryptedSharable = 100;
			public final static int UnencryptedNotSharable = 101;
		}
	}
}

