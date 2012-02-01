package org.witness.securesmartcam.io;

import org.witness.informa.utils.InformaConstants.Tables;

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
					"CREATE TABLE " + Tables.INFORMA_IMAGES + " (" + BaseColumns._ID + " " +
							"integer primary key autoincrement, " +
							Tables.Images.METADATA + " blob not null, " +
							Tables.Images.CONTAINMENT_ARRAY + " blob not null, " +
							Tables.Images.IMAGE_HASH + " text not null" +
							")",
					"CREATE TABLE " + Tables.INFORMA_CONTACTS + " (" + BaseColumns._ID + " " +
							"integer primary key autoincrement, " +
							Tables.Contacts.PSEUDONYM + " text not null, " +
							Tables.Contacts.DEFAULT_FILTER + " integer not null" +
							")",
					"CREATE TABLE " + Tables.INFORMA_SETUP + "(" + BaseColumns._ID + " " +
							"integer primary key autoincrement, " +
							Tables.Setup.SIG_KEY_ID + " text not null, " + 
							Tables.Setup.DEFAULT_SECURITY_LEVEL + " integer not null, " +
							Tables.Setup.LOCAL_TIMESTAMP + " integer not null, " +
							Tables.Setup.PUBLIC_TIMESTAMP + " integer not null" +
							")",
					"CREATE TABLE " + Tables.IMAGE_REGIONS + " (" + BaseColumns._ID + " " +
							"integer primary key autoincrement, " +
							Tables.Regions.KEY + " text not null, " +
							Tables.Regions.DATA + " blob not null" +
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
				getTable().compareTo(Tables.INFORMA_CONTACTS) == 0 ||
				getTable().compareTo(Tables.INFORMA_IMAGES) == 0 ||
				getTable().compareTo(Tables.INFORMA_SETUP) == 0 ||
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