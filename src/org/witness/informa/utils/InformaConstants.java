package org.witness.informa.utils;

public class InformaConstants {
	public final static String TAG = "************ INFORMA ***********";
	public final static String PW_EXPIRY = "**EXPIRED**";
	public final static int FROM_INFORMA_WIZARD = 3;
	
	public final static class Settings {
		public static final String INFORMA = "informa";
		public static final String SETTINGS_VIEWED = "informa.SettingsViewed";
		public static final String HAS_DB_PASSWORD = "informa.PasswordSet";
		public static final String DB_PASSWORD_CACHE_TIMEOUT = "informa.PasswordCacheTimeout";
		public static final String HAS_TRUSTED_ENDPOINTS = "informa.HasTrustedEndpoints";
	}
	
	public final static class Service {
		public final static String STOP_SERVICE = "stopService";
		public final static String SET_CURRENT = "setCurrent";
		public final static String SEAL_LOG = "sealLog";
	}

	public final static class Keys {
		public final static class Labels {
			public final static String CAPTURE_EVENT = "captureEvent";
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
	}
	
	public final static class Tables {
		public static final String INFORMA_IMAGES = "informaImages";
		public static final String INFORMA_CONTACTS = "informaContacts";
		public static final String INFORMA_SETUP = "informaSetup";
		public static final String IMAGE_REGIONS = "imageRegions";
		
		public final static class Images {
			public static final String METADATA = "metadata";
			public static final String CONTAINMENT_ARRAY = "containmentArray";
			public static final String IMAGE_HASH = "imageHash";
		}
		
		public final static class Contacts {
			public static final String PSEUDONYM = "pseudonym";
			public static final String DEFAULT_FILTER = "defaultFilter";
		}
		
		public final static class Setup {
			public static final String SIG_KEY_ID = "sigKeyID";
			public static final String DEFAULT_SECURITY_LEVEL = "defaultSecurityLevel";
			public static final String LOCAL_TIMESTAMP = "localTimestamp";
			public static final String PUBLIC_TIMESTAMP = "publicTimestamp";
		}
		
		public final static class Regions {
			public static final String KEY = "regionKey";
			public static final String DATA = "regionData";
		}
		
		
	}
}

