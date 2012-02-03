package org.witness.informa.utils;

public class InformaConstants {
	public final static String TAG = "************ INFORMA ***********";
	public final static String PW_EXPIRY = "**EXPIRED**";
	public final static int FROM_INFORMA_WIZARD = 3;
	public final static int FROM_INFORMA_TAGGER = 4;
	
	public final static class Settings {
		public static final String INFORMA = "informa";
		public static final String SETTINGS_VIEWED = "informa.SettingsViewed";
		public static final String HAS_DB_PASSWORD = "informa.PasswordSet";
		public static final String DB_PASSWORD_CACHE_TIMEOUT = "informa.PasswordCacheTimeout";
		public static final String DEFAULT_IMAGE_HANDLING = "informa.DefaultImageHandling";
	}
	
	public final static class Service {
		public final static String STOP_SERVICE = "stopService";
		public final static String SET_CURRENT = "setCurrent";
		public final static String SEAL_LOG = "sealLog";
	}

	public final static class Keys {
		public final static class Labels {
			public final static String CAPTURE_EVENT = "captureEvent";
			public final static String LOCATION_TYPE = "location_type";
			public final static String REGION_DATA = "region_data";
			public final static String REGION_KEY = "region_key";
			public final static String LOCAL_MEDIA_PATH = "source_localMeidaPath";
			public final static String ENCRYPT_LIST = "encryptList";
			public final static String MATCH_TIMESTAMP = "matchTimestamp";
			public final static String TIMESTAMP = "timestamp";
			public final static String MEDIA_TYPE = "source_type";

		}
		
		public final static class Suckers {
			public final static String PHONE = "Suckers_Phone";
			public final static String ACCELEROMETER = "Suckers_Accelerometer";
			public final static String GEO = "Suckers_Geo";
			
			public final static class Accelerometer {
				public final static String ACC = "acc";
				public final static String ORIENTATION = "orientation";
				public final static String LIGHT = "lightMeter";
				public final static String X = "acc_x";
				public final static String Y = "acc_y";
				public final static String Z = "acc_z";
				public final static String AZIMUTH = "orientation_azimuth";
				public final static String PITCH = "orientation_pitch";
				public final static String ROLL = "orientation_roll";
				public final static String LIGHT_METER_VALUE = "lightMeter_value";
			}
			
			public final static class Geo {
				public final static String GPS_COORDS = "location_gpsCoords";
			}
			
			public final static class Phone {
				public final static String CELL_ID = "location_cellId";
				public final static String IMEI = "device_imei";
				public final static String BLUETOOTH_DEVICE_NAME = "device_bluetooth_name";
				public final static String BLUETOOTH_DEVICE_ADDRESS = "device_bluetooth_address";
			}
		}
				
		public final static class MediaTypes {
			public final static int PHOTO = 101;
			public final static int VIDEO = 102;
		}
		
		public final static class CaptureEvents {
			public final static int MEDIA_CAPTURED = 5;
			public final static int MEDIA_SAVED = 6;
			public final static int REGION_GENERATED = 7;
			public final static int EXIF_REPORTED = 8;
		}

		public final static class LocationTypes {
			public final static int ON_MEDIA_CAPTURED = 9;
			public final static int ON_MEDIA_SAVED = 10;
			public final static int ON_REGION_GENERATED = 11;
		}

		public final static class SecurityLevels {
			public final static int UNENCRYPTED_SHARABLE = 100;
			public final static int UNENCRYPTED_NOT_SHARABLE = 101;
		}
		
		public final static class LoginCache {
			public final static int ALWAYS = 200;
			public final static int AFTER_SAVE = 201;
			public final static int ON_CLOSE = 202;
		}
		
		public final static class OriginalImageHandling {
			public final static int LEAVE_ORIGINAL_ALONE = 300;
			public final static int ENCRYPT_ORIGINAL = 301;
			public final static int DELETE_ORIGINAL = 302;
		}
	}
	
	public final static class Tables {
		public static final String INFORMA_IMAGES = "informaImages";
		public static final String INFORMA_CONTACTS = "informaContacts";
		public static final String INFORMA_SETUP = "informaSetup";
		public static final String IMAGE_REGIONS = "imageRegions";
		
		public final static class Images {
			public static final String METADATA = "source_metadata";
			public static final String CONTAINMENT_ARRAY = "source_containmentArray";
			public static final String IMAGE_HASH = "source_imageHash";
		}
		
		public final static class Contacts {
			public static final String PSEUDONYM = "subject_pseudonym";
			public static final String DEFAULT_FILTER = "subject_defaultFilter";
		}
		
		public final static class Setup {
			public static final String SIG_KEY_ID = "owner_sigKeyID";
			public static final String DEFAULT_SECURITY_LEVEL = "owner_defaultSecurityLevel";
			public static final String LOCAL_TIMESTAMP = "device_localTimestamp";
			public static final String PUBLIC_TIMESTAMP = "informa_publicTimestamp";
		}
		
		public final static class Regions {
			public static final String KEY = "region_key";
			public static final String DATA = "region_data";
		}
		
		
	}
}

