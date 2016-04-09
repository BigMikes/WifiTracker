package com.project.mps.wifitracker;

import android.net.Uri;
import android.os.AsyncTask;

import java.util.ArrayList;

/**
 * Created by luigi on 09/04/2016.
 */

public class ExportArffTask extends AsyncTask<DbManager, Integer, ArrayList<Uri>>{
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected ArrayList<Uri> doInBackground(DbManager... dbManagers) {
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

}