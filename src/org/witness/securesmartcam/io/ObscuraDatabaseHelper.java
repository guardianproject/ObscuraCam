package org.witness.securesmartcam.io;

import android.content.Context;
import android.provider.BaseColumns;
import info.guardianproject.database.sqlcipher.SQLiteDatabase;
import info.guardianproject.database.sqlcipher.SQLiteDatabase.CursorFactory;
import info.guardianproject.database.sqlcipher.SQLiteOpenHelper;

public class ObscuraDatabaseHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "obscura.db";
	private static final int DATABASE_VERSION = 1;
	
	public static final class TABLES {
		public static final String INFORMA_IMAGES = "informaImages";
		public static final String INFORMA_CONTACTS = "informaContacts";
		public static final String OBSCURA = "obscura";
		public static final String INFORMA_PREFERENCES = "informaPreferences";
	};
	
	private String TABLE;
	
	public enum QueryBuilders {
		INIT_INFORMA() {
			@Override
			public String[] build() {
				return new String[] {
					"CREATE TABLE " + TABLES.INFORMA_IMAGES + " (" + BaseColumns._ID + " integer primary key autoincrement, informa blob not null)",
					"CREATE TABLE " + TABLES.INFORMA_CONTACTS + " (" + BaseColumns._ID + " integer primary key autoincrement, pseudonym text not null)"
				};
			}
		},
		INIT_OBSCURA() {
			@Override
			public String[] build() {
				return new String[] {
					// TODO: i don't know how we construct this table yet...
					"CREATE TABLE " + TABLES.OBSCURA + " (" + BaseColumns._ID + ""	
				};
			}
		},
		INIT_PREFS() {
			@Override
			public String[] build() {
				return new String[] {
					"CREATE TABLE " + TABLES.INFORMA_PREFERENCES + "(" + BaseColumns._ID + " " +
					"integer primary key autoincrement, " +
					"destinationKeys blob not null, " + 
					"defaultSecurityLevel integer not null" +
					")"	
				};
			}
		};
		
		
		public abstract String[] build();
	}
	
	public ObscuraDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		
		
	}
	
	private void passwordMunger(String password) {
		// TODO: takes the password and encrypts it using a shallow method, 
		// in order to prevent pw from being stored in plaintext in preferences...
		
	}
	
	public void setTable(String whichTable) {
		TABLE = whichTable;
	}
	
	public String getTable() {
		return TABLE;
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if(oldVersion >= newVersion)
			return;
		
		String sql = null;
		if(oldVersion == 1)
			sql = "ALTER TABLE " + TABLE + " add note text;";
		if(oldVersion == 2)
			sql = "";
		
		if(sql != null)
			db.execSQL(sql);
		
	}

}
