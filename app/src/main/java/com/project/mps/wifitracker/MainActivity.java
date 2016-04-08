package com.project.mps.wifitracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initialization
        dbm = new DbManager(getApplicationContext());
        measuring = false;
        numberOfSamples = 10;
        WifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        WifiRec = new WifiReceiver();


        registerReceiver(WifiRec, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        //If the wifi is turned off, it will be activated
        if(!WifiManager.isWifiEnabled())
        {
            WifiManager.setWifiEnabled(true);
            Toast.makeText(this, "Activating Wifi module", Toast.LENGTH_LONG).show();
        }

        //Set the listener for the start measuring button

        Button btSave = (Button) findViewById(R.id.button_start);
        btSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("BUTTON HANDLER", "start");
                scanWifi();
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
    }

    //TODO: for the moment doesn't work since the db is empty remember to test when full
    public void sendDB() {
        Log.v("SEND_DB", "start");
        Intent i = new Intent(Intent.ACTION_SEND);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Log.v("SEND_DB", "db: " + getDatabasePath(dbm.getDbName()));
        i.putExtra(Intent.EXTRA_STREAM, getDatabasePath("measures"));
        i.setType("application//octet-stream");
        startActivity(Intent.createChooser(i, "Export DB"));
    }

    class WifiReceiver extends BroadcastReceiver
    {
        public void onReceive(Context c, Intent intent)
        {
            if(measuring == true){
                numberOfSamples--;
                if(numberOfSamples == 0) {
                    //TODO: penso che qui ci vada un "measuring == false"
                    stopScan();
                }
            }
            Long tsLong = System.currentTimeMillis()/1000;
            String ts = tsLong.toString();
            String info = "";
            List<ScanResult> results;
            results = WifiManager.getScanResults();
            Log.i(TAG, "Sample: " + numberOfSamples + " TIME: " + ts + " SCAN: \n");
            for(int i = 0; i < results.size(); i++){
                info += "BSSID: " + results.get(i).BSSID + " SSID: " + results.get(i).SSID + " Level: "
                        + results.get(i).level + " Frequency: "  + results.get(i).frequency;
                Log.i(TAG, info);
                info = "";
            }
            Log.i(TAG, "END");

        }
    }
}
