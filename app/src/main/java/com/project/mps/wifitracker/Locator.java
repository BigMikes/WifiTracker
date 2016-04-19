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
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static android.widget.Toast.LENGTH_SHORT;

public class Locator extends AppCompatActivity implements View.OnClickListener{
    private WifiManager WifiManager;
    private WifiReceiver WifiRec;
    private SocketClient socket;
    private final String ServerAddress = "IPADDRESS";
    private final int ServerPort = 8888;
    private static final String TAG = "Locator";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locator);
        //Initiate the local variable
        WifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        WifiRec = new WifiReceiver();
        socket = null;

        //Check if the wifi model has been turned on
        registerReceiver(WifiRec, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        //If the wifi is turned off, it will be activated
        if(!WifiManager.isWifiEnabled())
        {
            WifiManager.setWifiEnabled(true);
            Toast.makeText(this, "Activating Wifi module", Toast.LENGTH_LONG).show();
        }

        //Set on click listener
        Button btStart = (Button) findViewById(R.id.query_button);
        Button btExport = (Button) findViewById(R.id.contribute_button);
        try {
            assert btStart != null;
            btStart.setOnClickListener(this);
            assert btExport != null;
            btExport.setOnClickListener(this);
        } catch (AssertionError ae) {
            Log.v("ASSERTION_ERROR: ", ae.getMessage());
            Toast.makeText(Locator.this, "Button not reachable: ", LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.query_button:
                //TODO: per il momento manda un campione solo, ma forse si potrebbero mandare più samples o aggregarli con una media
                WifiManager.startScan();
                break;
            case R.id.contribute_button:
                //Start the new activity for the contribution process
                Intent intent = new Intent(this, Contribution.class);
                startActivity(intent);
                break;
        }
    }

    private void startQuery() {


    }

    class WifiReceiver extends BroadcastReceiver{

        public void onReceive(Context c, Intent intent){
            List<ScanResult> results;
            results = WifiManager.getScanResults();
            List<WifiInfo> samples = new ArrayList<WifiInfo>();
            for(int i = 0; i < results.size(); i++) {
                Integer freq = results.get(i).frequency;
                Integer level = results.get(i).level;
                WifiInfo toAdd = new WifiInfo(results.get(i).BSSID, results.get(i).SSID, freq.toString(), level.toString());
                samples.add(toAdd);
            }
            socket = new SocketClient(ServerAddress, ServerPort);
            if(socket == null)
                return;
            if(!socket.sendQuery(samples))
                return;
            String response = socket.readLine();
            socket.closeSocket();
            if(response == null) {
                Log.v(TAG, "Problems in retrieving the response from the server");
                return;
            }
            TextView text = (TextView) findViewById(R.id.text_response);
            text.setText("You are in: " + response);

        }
    }
}
