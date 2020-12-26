package net.justdave.mcstatus.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class Database extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "serverlist.db";
	private static final int DATABASE_VERSION = 1;

	public Database(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL("CREATE TABLE serverlist ("
				+ "id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "servername TEXT NOT NULL, "
				+ "serveraddress TEXT NOT NULL UNIQUE"
				+ ");");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(Database.class.getName(), "Upgrading database from version "
				+ oldVersion + " to " + newVersion
				+ ", which will destroy all old data");
		// if we ever actually upgrade the schema, we can do stuff here instead
		// of dropping it
		db.execSQL("DROP TABLE IF EXISTS serverlist");
		onCreate(db);
	}
}