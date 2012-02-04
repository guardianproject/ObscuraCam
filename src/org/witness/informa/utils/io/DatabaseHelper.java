package org.witness.informa.utils.io;

import org.witness.informa.utils.InformaConstants.Keys.*;

import info.guardianproject.database.sqlcipher.SQLiteDatabase;
import info.guardianproject.database.sqlcipher.SQLiteOpenHelper;
import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;

public class DatabaseHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "informa.db";
	private static final int DATABASE_VERSION = 1;
	
	public static String TABLE;
	
	public enum QueryBuilders {
		INIT_INFORMA() {
			@Override
			public String[] build() {
				return new String[] {
					"CREATE TABLE " + Tables.IMAGES + " (" + BaseColumns._ID + " " +
							"integer primary key autoincrement, " +
							Image.METADATA + " blob not null, " +
							Image.CONTAINMENT_ARRAY + " blob not null, " +
							Image.UNREDACTED_IMAGE_HASH + " text not null, " +
							Image.REDACTED_IMAGE_HASH + " text not null, " +
							Image.LOCATION_OF_ORIGINAL + " text not null" +
							")",
					"CREATE TABLE " + Tables.CONTACTS + " (" + BaseColumns._ID + " " +
							"integer primary key autoincrement, " +
							ImageRegion.Subject.PSEUDONYM + " text not null, " +
							ImageRegion.Subject.PERSIST_FILTER + " integer not null" +
							")",
					"CREATE TABLE " + Tables.SETUP + "(" + BaseColumns._ID + " " +
							"integer primary key autoincrement, " +
							Owner.SIG_KEY_ID + " text not null, " + 
							Owner.DEFAULT_SECURITY_LEVEL + " integer not null, " +
							Device.LOCAL_TIMESTAMP + " integer not null, " +
							Device.PUBLIC_TIMESTAMP + " integer not null, " +
							Owner.OWNERSHIP_TYPE + " integer not null" +
							")",
					"CREATE TABLE " + Tables.IMAGE_REGIONS + " (" + BaseColumns._ID + " " +
							"integer primary key autoincrement, " +
							ImageRegion.KEY + " text not null, " +
							ImageRegion.DATA + " blob not null" +
							")"
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
	
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		
	}

	@Override
	public void onCreate(SQLiteDatabase db) {}
	
	public Cursor getValue(SQLiteDatabase db, String[] values, String matchKey, Object matchValue) {
		String select = "*";
		
		if(values != null) {
			StringBuffer sb = new StringBuffer();
			for(String v : values)
				sb.append(v + ",");
			select = sb.toString().substring(0, sb.toString().length() - 1);
		}
		
		if(matchValue.getClass().equals(String.class))
			matchValue = "\"" + matchValue + "\"";
		
		Cursor c = db.rawQuery("SELECT " + select + " FROM " + getTable() + " WHERE " + matchKey + " = " + matchValue, null);
		if(c != null && c.getCount() > 0) {
			return c;
		} else
			return null;
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
				getTable().compareTo(Tables.CONTACTS) == 0 ||
				getTable().compareTo(Tables.IMAGES) == 0 ||
				getTable().compareTo(Tables.SETUP) == 0 ||
				getTable().compareTo(Tables.IMAGE_REGIONS) == 0
			)
				queries = QueryBuilders.INIT_INFORMA.build();
			
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