package com.project.mps.wifitracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.util.UUID;

/**
 * Created by Luigi on 07/04/2016.
 */
public class DbManager extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "measures";

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

    public DbManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.v("DBManager", "constructor");
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

    public void store (Measurement measureList) {
        Log.v("DBManager", "store");

        SQLiteDatabase db = this.getWritableDatabase();

        for(WifiInfo wf : measureList.getSamples()) {
            ContentValues values = new ContentValues();
            values.put(KEY_ID_MEASURE, UUID.randomUUID().toString());
            values.put(KEY_EDIFICIO, measureList.getBuilding());
            values.put(KEY_PIANO, measureList.getFloor());
            values.put(KEY_AULA, measureList.getRoom());
            values.put(KEY_BSSID, wf.getBssid());
            values.put(KEY_SSID, wf.getSsid());
            values.put(KEY_FREQUENCY, wf.getFrequency());
            values.put(KEY_RSSI, wf.getRssi());

            db.insert(TABLE_MEASURES, null, values);
        }

        db.close();
    }

}
