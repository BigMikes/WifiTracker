package com.project.mps.wifitracker;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private WifiManager WifiManager;
    private WifiReceiver WifiRec;
    private final Handler handler= new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        WifiRec = new WifiReceiver();
        registerReceiver(WifiRec, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        //If the wifi is turned off, it will be activated
        if(WifiManager.isWifiEnabled() == false)
        {
            WifiManager.setWifiEnabled(true);
            Toast.makeText(this, "Wifi is off, activating...", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    protected void onPause()
    {
        unregisterReceiver(WifiRec);
        super.onPause();
    }

    @Override
    protected void onResume()
    {
        registerReceiver(WifiRec, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }
}
