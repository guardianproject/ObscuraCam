package org.witness.informa.utils;

public class InformaConstants {
	public final static String TAG = "************ INFORMA ***********";
	public final static String READOUT = "******************* INFORMA READOUT ******************";
	public final static String PW_EXPIRY = "**EXPIRED**";
	public final static int FROM_INFORMA_WIZARD = 3;
	public final static int FROM_INFORMA_TAGGER = 4;
	public final static int FROM_TRUSTED_DESTINATION_CHOOSER = 5;
	public final static int BLOB_MAX = 1048576;

	public final static class Keys {
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
		
		public final static class Informa {
			public final static String INTENT = "intent";
			public final static String GENEALOGY = "genealogy";
			public final static String DATA = "data";
		}
		
		public final static class CaptureEvent {
			public final static String TYPE = "captureEvent";
			public final static String MATCH_TIMESTAMP = "matchTimestamp";
			public final static String TIMESTAMP = "timestamp";
		}
		
		public final static class ImageRegion {
			public final static String INDEX = "regionIndex";
			public final static String THUMBNAIL = "regionThumbnail";
			public static final String KEY = "region_key";
			public static final String DATA = "region_data";
			public final static String TIMESTAMP = "timestampOnGeneration";
			public final static String LOCATION = "locationOnGeneration";
			public final static String TAGGER_RETURN = "taggerReturned";
			public final static String FILTER = "region_obfuscationType";
			public final static String COORDINATES = "region_initialCoordinates";
			public final static String WIDTH = "region_width";
			public final static String HEIGHT = "region_height";
			public final static String TOP = "region_top";
			public final static String LEFT = "region_left";
			public final static String UNREDACTED_HASH = "region_unredactedHash";
			
			public final static class Subject {
				public final static String PSEUDONYM = "subject_pseudonym";
				public final static String INFORMED_CONSENT_GIVEN = "subject_informedConsentGiven";
				public final static String PERSIST_FILTER = "subject_persistFilter";
			}
		}
		
		public final static class Location {
			public final static String TYPE = "location_type";
			public final static String COORDINATES = "location_gpsCoordinates";
			public final static String CELL_ID = Suckers.Phone.CELL_ID;
		}
		
		public final static class Intent {
			public final static String ENCRYPT_LIST = "encryptList";
			public final static class Destination {
				public final static String EMAIL = "destinationEmail";
				public final static String DISPLAY_NAME = "displayName";
			}
		}
		
		public final static class TrustedDestinations {
			public final static String EMAIL = Intent.Destination.EMAIL;
			public final static String KEYRING_ID = "keyringId";
			public final static String DISPLAY_NAME = Intent.Destination.DISPLAY_NAME;
		}
		
		public final static class Image {
			public static final String METADATA = "source_metadata";
			public static final String CONTAINMENT_ARRAY = "source_containmentArray";
			public static final String UNREDACTED_IMAGE_HASH = "source_unredactedImageHash";
			public static final String REDACTED_IMAGE_HASH = "source_redactedImageHash";
			public final static String MEDIA_TYPE = "source_type";
			public final static String LOCAL_MEDIA_PATH = "source_localMeidaPath";
			public final static String TIMESTAMP = "timestamp";
			public final static String LOCATION_OF_ORIGINAL = "source_locationOfOriginal";
		}
		
		public final static class Owner {
			public static final String SIG_KEY_ID = "owner_sigKeyID";
			public static final String DEFAULT_SECURITY_LEVEL = "owner_defaultSecurityLevel";
			public static final String OWNERSHIP_TYPE = "owner_ownershipType";
		}
		
		public final static class Device {
			public static final String LOCAL_TIMESTAMP = "device_localTimestamp";
			public static final String PUBLIC_TIMESTAMP = "device_publicTimestamp";
			public static final String IMEI = Suckers.Phone.IMEI;
			public static final String BLUETOOTH_DEVICE_NAME = Suckers.Phone.BLUETOOTH_DEVICE_NAME;
			public static final String BLUETOOTH_DEVICE_ADDRESS = Suckers.Phone.BLUETOOTH_DEVICE_ADDRESS;
		}
		
		public final static class Tables {
			public static final String IMAGES = "informaImages";
			public static final String CONTACTS = "informaContacts";
			public static final String SETUP = "informaSetup";
			public static final String IMAGE_REGIONS = "imageRegions";
			public static final String TRUSTED_DESTINATIONS = "trustedDestinations";
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
				public final static String GPS_COORDS = Location.COORDINATES;
			}
			
			public final static class Phone {
				public final static String CELL_ID = "location_cellId";
				public final static String IMEI = "device_imei";
				public final static String BLUETOOTH_DEVICE_NAME = "device_bluetooth_name";
				public final static String BLUETOOTH_DEVICE_ADDRESS = "device_bluetooth_address";
			}
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
	
	public final static class Device {
		public final static int IS_SELF = -1;
		public final static int IS_NEIGHBOR = 1;
	}
	
	public final static class Owner {
		public final static int INDIVIDUAL = 400;
	}
	
	public final static class Consent {
		public final static int GENERAL = 101;
	}
	
	public final static class Selections {
		public final static String SELECT_ONE = "select_one";
		public final static String SELECT_MULTI = "select_multi";
	}
}

