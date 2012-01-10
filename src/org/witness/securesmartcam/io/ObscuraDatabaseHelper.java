package org.witness.securesmartcam.io;

import org.witness.sscphase1.ObscuraApp;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.util.Log;
import info.guardianproject.database.sqlcipher.SQLiteDatabase;
import info.guardianproject.database.sqlcipher.SQLiteDatabase.CursorFactory;
import info.guardianproject.database.sqlcipher.SQLiteException;
import info.guardianproject.database.sqlcipher.SQLiteOpenHelper;

public class ObscuraDatabaseHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "obscura.db";
	private static final int DATABASE_VERSION = 1;
	
	public static final class TABLES {
		public static final String INFORMA_IMAGES = "informaImages";
		public static final String INFORMA_CONTACTS = "informaContacts";
		public static final String OBSCURA = "obscura";
		public static final String OBSCURA_BITS = "obscuraBits";
		public static final String INFORMA_PREFERENCES = "informaPreferences";
		public static final String IMAGE_REGIONS = "imageRegions";
	};
	
	public static String TABLE;
	
	public enum QueryBuilders {
		INIT_INFORMA() {
			@Override
			public String[] build() {
				return new String[] {
					"CREATE TABLE " + TABLES.INFORMA_IMAGES + " (" + BaseColumns._ID + " integer primary key autoincrement, informa blob not null)",
					"CREATE TABLE " + TABLES.INFORMA_CONTACTS + " (" + BaseColumns._ID + " integer primary key autoincrement, pseudonym text not null)",
					"CREATE TABLE " + TABLES.INFORMA_PREFERENCES + "(" + BaseColumns._ID + " " +
							"integer primary key autoincrement, " +
							"destinationKeys blob not null, " + 
							"defaultSecurityLevel integer not null" +
							")",
					"CREATE TABLE " + TABLES.IMAGE_REGIONS + " (" + BaseColumns._ID + " integer primary key autoincrement, regionKey text not null, regionData blob not null)"
				};
			}
		},
		INIT_OBSCURA() {
			@Override
			public String[] build() {
				return new String[] {
					"CREATE TABLE " + TABLES.OBSCURA + " (" + BaseColumns._ID + " integer primary key autoincrement, imageHash text not null, containmentArray blob not null, metadata blob not null)",
					"CREATE TABLE " + TABLES.OBSCURA_BITS + " (" + BaseColumns._ID + " integer primary key autoincrement, hash text not null, data blob not null)"
				};
			}
		},
		CHECK_IF() {
			@Override
			public String[] build() {
				return new String[] {
					"SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name= '" + TABLE + "'" 
				};
			}
		};
		
		public abstract String[] build();
	}
	
	public ObscuraDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		
	}

	@Override
	public void onCreate(SQLiteDatabase db) {}
	
	private void passwordMunger(String password) {
		// TODO: takes the password and encrypts it using a shallow method, 
		// in order to prevent pw from being stored in plaintext in preferences...
		
	}
	
	public boolean setTable(SQLiteDatabase db, String whichTable) {
		TABLE = whichTable;
		
		Cursor c = db.rawQuery(QueryBuilders.CHECK_IF.build()[0], null);
		if(c != null && c.getCount() > 0) {
			c.close();
			return true;
		} else {
			c.close();
			String[] queries = null;
			if(
				getTable().compareTo(TABLES.INFORMA_CONTACTS) == 0 ||
				getTable().compareTo(TABLES.INFORMA_IMAGES) == 0 ||
				getTable().compareTo(TABLES.INFORMA_PREFERENCES) == 0 ||
				getTable().compareTo(TABLES.IMAGE_REGIONS) == 0
			)
				queries = QueryBuilders.INIT_INFORMA.build();
			else if(
				getTable().compareTo(TABLES.OBSCURA) == 0 ||
				getTable().compareTo(TABLES.OBSCURA_BITS) == 0
			)
				queries = QueryBuilders.INIT_OBSCURA.build();
		
			for(String q : queries)
				db.execSQL(q);
		}
		return false;
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
