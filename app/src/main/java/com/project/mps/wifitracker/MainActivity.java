package com.project.mps.wifitracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MAIN";
    private WifiManager WifiManager;
    private WifiReceiver WifiRec;
    private Timer timerTask;
    private boolean measuring;
    private int numberOfSamples;
    private DbManager dbm;
    private Measurement measurement;
    private ProgressBar mProgress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initialization
        dbm = new DbManager(getApplicationContext());
        measuring = false;
        numberOfSamples = 0;
        measurement = null;
        WifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        WifiRec = new WifiReceiver();
        mProgress = (ProgressBar) findViewById(R.id.progressBar);
        mProgress.setProgress(0);

        registerReceiver(WifiRec, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        //If the wifi is turned off, it will be activated
        if(!WifiManager.isWifiEnabled())
        {
            WifiManager.setWifiEnabled(true);
            Toast.makeText(this, "Activating Wifi module", Toast.LENGTH_LONG).show();
        }

        //Set the listener for the start measuring button

        Button btStart = (Button) findViewById(R.id.button_start);
        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("BUTTON HANDLER", "start");
                EditText building = (EditText) findViewById(R.id.input_building);
                EditText floor = (EditText) findViewById(R.id.input_floor);
                EditText room = (EditText) findViewById(R.id.input_room);
                EditText nSamples = (EditText) findViewById(R.id.input_num_samp);
                //TODO: aggiungere controllo che non fa partire niente se i campi non sosno pieni altrimenti da errori strani
                numberOfSamples = Integer.parseInt(nSamples.getText().toString());
                measurement = new Measurement(building.getText().toString(), floor.getText().toString(), room.getText().toString(),null);
                mProgress.setMax(numberOfSamples);
                scanWifi();
            }
        });
        Button btExport = (Button) findViewById(R.id.button_export);
        btExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("BUTTON HANDLER", "export");
                dbm.LogDb();
                dbm.exportDb();
            }
        });
    }

    @Override
    protected void onPause() {
        unregisterReceiver(WifiRec);
        super.onPause();
    }

    @Override
    protected void onResume() {
        registerReceiver(WifiRec, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    public void scanWifi(){
        measuring = true;
        timerTask = new Timer();
        timerTask.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                WifiManager.startScan();
            }
        }, 0, 1000);
    }

    public void stopScan(){
        measuring = false;
        timerTask.cancel();
        timerTask.purge();
        timerTask = null;
        //TODO: prendere i dati e salvarli nel database
        /*dbm.store(measurement);
        String[] result = measurement.print();
        for(String i : result){
            Log.i(TAG, i);
        }*/
        mProgress.setProgress(0);
        measurement = null;
    }

    //Intent ACTION_SEND cannot be sent from outside an activity so it must be implementede here
    public void sendDB() {
        Log.v("SEND_DB", "start");
        Intent i = new Intent(Intent.ACTION_SEND);
        //i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(getDatabasePath(dbm.getDbName())));
        i.setType("application//octet-stream");
        startActivity(Intent.createChooser(i, "Export DB"));
    }

    class WifiReceiver extends BroadcastReceiver
    {

        public void onReceive(Context c, Intent intent)
        {
            if(measuring) {
                numberOfSamples--;
                Long tsLong = System.currentTimeMillis() / 1000;
                String ts = tsLong.toString();
                String info = "";
                List<ScanResult> results;
                results = WifiManager.getScanResults();

                Log.i(TAG, "Sample: " + numberOfSamples + " TIME: " + ts + " SCAN: \n");
                for (int i = 0; i < results.size(); i++) {
                    /*------------------------------------DEBUG-------------------------------*/
                    info += "BSSID: " + results.get(i).BSSID + " SSID: " + results.get(i).SSID + " Level: "
                            + results.get(i).level + " Frequency: " + results.get(i).frequency;
                    Log.i(TAG, info);
                    info = "";
                    /*------------------------------------------------------------------------*/

                    Integer freq = results.get(i).frequency;
                    Integer level = results.get(i).level;
                    WifiInfo toAdd = new WifiInfo(results.get(i).BSSID, results.get(i).SSID, freq.toString(), level.toString());
                    measurement.addSample(toAdd);
                    mProgress.setProgress(mProgress.getMax() - numberOfSamples);
                }
                dbm.store(measurement);
                measurement.deleteSamples();
                Log.i(TAG, "END");
                if (numberOfSamples == 0) {
                    stopScan();
                }
            }
        }
    }
}
