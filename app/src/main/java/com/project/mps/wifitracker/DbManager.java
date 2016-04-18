package com.project.mps.wifitracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by Luigi on 07/04/2016.
 */
public class DbManager extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "measures";

    //context
    private static Context context;

    // Contacts table name
    private static final String TABLE_MEASURES = "campioni";

    // Contacts Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_ID_MEASURE = "id_measure";
    private static final String KEY_EDIFICIO = "edificio";
    private static final String KEY_PIANO = "piano";
    private static final String KEY_AULA = "aula";
    private static final String KEY_BSSID = "bssid";
    private static final String KEY_SSID = "ssid";
    private static final String KEY_FREQUENCY = "frequency";
    private static final String KEY_RSSI = "rssi";

    public DbManager(Context c) {
        super(c, DATABASE_NAME, null, DATABASE_VERSION);
        Log.v("DBManager", "constructor");
        context = c;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.v("DBManager", "oncreate");
        String CREATE_MEASURES_TABLE = "CREATE TABLE " + TABLE_MEASURES
                + "("
                    + KEY_ID            + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,"
                    + KEY_ID_MEASURE    + " TEXT NOT NULL,"
                    + KEY_EDIFICIO      + " TEXT NOT NULL,"
                    + KEY_PIANO         + " TEXT NOT NULL,"
                    + KEY_AULA          + " TEXT NOT NULL,"
                    + KEY_BSSID         + " TEXT NOT NULL,"
                    + KEY_SSID          + " TEXT NOT NULL,"
                    + KEY_FREQUENCY     + " TEXT NOT NULL,"
                    + KEY_RSSI          + " TEXT NOT NULL"
                + ")";
        db.execSQL(CREATE_MEASURES_TABLE);
        Log.v("DBManager", "oncreate end");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion != newVersion){
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEASURES);
        }
        onCreate(db);
    }

    public boolean store(Measurement measureList) {
        Log.v("DBManager", "store");

        String measureId = UUID.randomUUID().toString();

        SQLiteDatabase db = this.getWritableDatabase();

        for(WifiInfo wf : measureList.getSamples()) {
            ContentValues values = new ContentValues();
            values.put(KEY_ID_MEASURE, measureId);
            values.put(KEY_EDIFICIO, measureList.getBuilding());
            values.put(KEY_PIANO, measureList.getFloor());
            values.put(KEY_AULA, measureList.getRoom());
            values.put(KEY_BSSID, wf.getBssid());
            values.put(KEY_SSID, wf.getSsid());
            values.put(KEY_FREQUENCY, wf.getFrequency());
            values.put(KEY_RSSI, wf.getRssi());

            long temp = db.insert(TABLE_MEASURES, null, values);
            if(temp == -1) {
                return false;
            }
        }

        db.close();
        return true;
    }

    public String getDbName() {
        return DATABASE_NAME;
    }

    public void LogDb() {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_MEASURES, null);
        try {
            Log.v("LogDb", DatabaseUtils.dumpCursorToString(c));
        } finally {
            c.close();
        }
        db.close();
    }

    //TODO: ine function to return db and one to get list on building and one for mac of each buldings
    public ArrayList<String> getBuildings() {
        Log.v("getBuildings", "start");
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<String> building = new ArrayList<>();

        Cursor c = db.query(true, TABLE_MEASURES, new String[]{KEY_EDIFICIO}, null, null, null, null, null, null);
        Log.v("getBuildings", DatabaseUtils.dumpCursorToString(c));
        try{
            while(c.moveToNext()) {
                building.add(c.getString(c.getColumnIndex(KEY_EDIFICIO)));
            }
        } finally {
            c.close();
        }
        db.close();
        Log.v("getBuildings", building.toString());
        return building;
    }

    public ArrayList<String> getBssid(String building) {
        Log.v("getBssid", "start");
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<String> macList = new ArrayList<>();

        Cursor c = db.query(true, TABLE_MEASURES, new String[]{KEY_BSSID}, KEY_EDIFICIO +" = ?", new String[]{building}, null, null, null, null, null);
        Log.v("getBssid", DatabaseUtils.dumpCursorToString(c));
        try{
            while(c.moveToNext()) {
                macList.add(c.getString(c.getColumnIndex(KEY_BSSID)));
            }
        } finally {
            c.close();
        }
        db.close();
        Log.v("getBssid", macList.toString());
        return macList;
    }

    public int getNumberOfBuildings() {
        Log.v("getNumberOfBuildings", "start");
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = db.query(true, TABLE_MEASURES, new String[]{KEY_EDIFICIO}, null, null, null, null, null, null, null);
        int result = c.getCount();
        c.close();
        db.close();
        Log.v("getNumberOfBuildings", Integer.toString(result));
        return result;
    }

    public void exportDb(){
        Log.v("exportDb","START");
        try {
            String root = Environment.getExternalStorageDirectory().toString();
            File sd = new File(root + "/WiFi Tracker");
            if(!sd.exists()){
                if (!sd.mkdirs()) {
                    Log.e("exportDb", "Directory not created");
                }
            }
            Log.v("exportDb","sd: " + sd);
            File data = Environment.getDataDirectory();
            Log.v("exportDb","data: " + data.getPath());

            if (sd.canWrite()) {
                //String currentDBPath = "//data//com.project.mps.wifitracker//databases//measures";
                String currentDBPath = getDbPath();
                Log.v("exportDb","currentDBPath: " + currentDBPath);
                String backupDBPath = "BackupDB";
                File currentDB = new File(data, currentDBPath);
                File backupDB = new File(sd, backupDBPath);
                if(!backupDB.exists()) {
                    if (!backupDB.createNewFile()) {
                        Log.e("exportDb", "File not created");
                    }
                }
                else{
                    backupDB.delete();
                }
                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                }
            }
        } catch (Exception e) {
            Log.v("ERROR", e.toString());
        }

    }

    public static String getDbPath() {
        return context.getDatabasePath(DATABASE_NAME).getPath().replace("/data/data", "/data");
    }

    public void emptyDb() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MEASURES, null, null);
    }

}
