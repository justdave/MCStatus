package net.justdave.mcstatus;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.net.URISyntaxException;
import java.util.ArrayList;

public class ServerDB {
    private static final String TAG = ServerDB.class.getSimpleName();
    final private Database dbHelper;
    private SQLiteDatabase database;

    public ServerDB(Context context) {
        dbHelper = new Database(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void create(String serverName, String serverAddress) {
        ContentValues values = new ContentValues();
        values.put("servername", serverName);
        values.put("serveraddress", serverAddress);
        database.insert("serverlist", null, values);
    }

    public void delete(String serverAddress) {
        database.delete("serverlist", "serveraddress = ?",
                new String[]{serverAddress});
    }

    public void update(String oldServerAddress, String newServerAddress, String newServerName) {
        ContentValues args = new ContentValues();
        args.put("serveraddress", newServerAddress);
        args.put("servername", newServerName);
        database.update("serverlist", args, "serveraddress = ?", new String[]{oldServerAddress});
    }

    public void getAllServers(ArrayList<MinecraftServer> serverlist) {
        Log.i(TAG, "getAllServers() called");
        serverlist.clear();
        Cursor cursor = database.query("serverlist", new String[]{
                "servername", "serveraddress"}, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String serverName = cursor.getString(cursor
                    .getColumnIndexOrThrow("servername"));
            String serverAddress = cursor.getString(cursor
                    .getColumnIndexOrThrow("serveraddress"));

            MinecraftServer server;
            try {
                server = new MinecraftServer(serverName, serverAddress);
                serverlist.add(server);
            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            cursor.moveToNext();
        }
        cursor.close();
    }
}