package com.project.mps.wifitracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
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

    private static final String ServerAddress = "192.168.1.20";
    private static final int ServerPort = 8888;
    private static final String TAG = "Locator";
    private List<WifiInfo> lastSamples;
    private boolean onQuerying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locator);
        //Initiate the local variable
        WifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        WifiRec = new WifiReceiver();
        onQuerying = false;

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
    protected void onPause() {
        unregisterReceiver(WifiRec);
        super.onPause();
    }


    @Override
    protected void onResume() {
        registerReceiver(WifiRec, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.query_button:
                //TODO: per il momento manda un campione solo, ma forse si potrebbero mandare più samples o aggregarli con una media
                if(checkInternetConnectivity()) {
                    onQuerying = true;
                    WifiManager.startScan();
                }
                break;
            case R.id.contribute_button:
                //Start the new activity for the contribution process
                Intent intent = new Intent(this, Contribution.class);
                startActivity(intent);
                break;
        }
    }

    public boolean checkInternetConnectivity() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            Toast.makeText(this, "There is no Internet connectivity, please connect", Toast.LENGTH_SHORT).show();
            return false;
        }
    }


    class WifiReceiver extends BroadcastReceiver{

        public void onReceive(Context c, Intent intent){
            Log.v(TAG, "BREAK POINT");
            List<ScanResult> results;
            results = WifiManager.getScanResults();
            lastSamples = new ArrayList<WifiInfo>();
            for(int i = 0; i < results.size(); i++) {
                Integer freq = results.get(i).frequency;
                Integer level = results.get(i).level;
                WifiInfo toAdd = new WifiInfo(results.get(i).BSSID, results.get(i).SSID, freq.toString(), level.toString());
                lastSamples.add(toAdd);
            }
            if(onQuerying)
                new AsyncQuery().execute(lastSamples);
            onQuerying = false;
        }
    }

    private class AsyncQuery extends AsyncTask<List<WifiInfo>,Void, String>{
        private SocketClient socket;

        @Override
        protected String doInBackground(List<WifiInfo>... params) {
            socket = new SocketClient(ServerAddress, ServerPort);
            if(socket == null) {
                Log.e(TAG, "Problems creating the socket");
                return null;
            }
            if(!socket.SocketConnect()){
                Log.e(TAG, "Problems in connecting to the server");
                return null;
            }
            if(!socket.sendQuery(lastSamples)) {
                Log.e(TAG, "Problems in sending the query to the server");
                return null ;
            }
            String response = socket.readLine();
            socket.closeSocket();
            if(response == null) {
                Log.e(TAG, "Problems in retrieving the response from the server");
                return null;
            }
            return response;
        }

        @Override
        protected void onPostExecute(String s) {
            TextView toShow = (TextView) findViewById(R.id.text_response);
            toShow.setText(s);
        }
    }
}
