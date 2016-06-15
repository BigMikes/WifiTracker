package com.project.mps.wifitracker;

import android.app.ProgressDialog;
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
import java.util.Timer;
import java.util.TimerTask;

import static android.widget.Toast.LENGTH_SHORT;

public class Locator extends AppCompatActivity implements View.OnClickListener{
    private WifiManager WifiManager;
    private WifiReceiver WifiRec;

    private static final String ServerAddress = "192.168.1.19";
    //private static final String ServerAddress = "ciaoasdfghjkl.ddns.net";
    private static final int ServerPort = 8080;
    private static final String TAG = "Locator";
    private List<WifiInfo> lastSample;
    private List<List<WifiInfo>> finalSamples;
    private boolean onQuerying;
    private int NUM_SAMPLES;
    private int iteration;
    private ProgressDialog progress;
    private Timer timerTask;
    private int timeOutInterval = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locator);
        //Initiate the local variable
        WifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        WifiRec = new WifiReceiver();
        onQuerying = false;
        NUM_SAMPLES = 3;
        finalSamples = new ArrayList<List<WifiInfo>>(NUM_SAMPLES);
        iteration = 0;

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
                //The user wants to know where he/she is
                if(checkInternetConnectivity()) {
                    //Start the waiting dialog
                    progress = new ProgressDialog(this);
                    progress.setMessage("Sampling...");
                    progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    progress.setIndeterminate(true);
                    progress.setCancelable(false);
                    progress.setCanceledOnTouchOutside(false);
                    progress.show();
                    //Start sampling wifi fingerprints
                    startSampling();
                }
                break;
            case R.id.contribute_button:
                //Start the new activity for the contribution process
                Intent intent = new Intent(this, Contribution.class);
                startActivity(intent);
                break;
        }
    }

    //It initializes the sampling process
    private void startSampling(){
        TextView toShow = (TextView) findViewById(R.id.text_response);
        toShow.setText("Scanning...");
        //Set the flag
        onQuerying = true;
        //In order to receive wifi samples we need to ask periodically to the manager for them
        timerTask = new Timer();
        timerTask.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                WifiManager.startScan();
            }
        }, 0, 500);
    }

    //After it's collected N samples it stops sampling
    private void stopSampling(){
        //Reset the flag
        onQuerying = false;
        //Clear the timer
        timerTask.cancel();
        timerTask.purge();
        timerTask = null;
        iteration = 0;
        progress.setMessage("Asking to the server...");
        //Create an asyncTask to send the collected samples to the central server
        new AsyncQuery().execute(finalSamples);

    }

    //Function to check if the device is connected
    private boolean checkInternetConnectivity() {
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

    //Private class that implements the broadcast receiver of wifi scan results intent
    class WifiReceiver extends BroadcastReceiver{
        //Everytime it receives an intent it means that a wifi scan is ready to be read
        public void onReceive(Context c, Intent intent){
            //Check if it is interested in collecting samples
            if(!onQuerying)
                return;
            Log.v(TAG, "BREAK POINT");
            List<ScanResult> results;
            results = WifiManager.getScanResults();
            //Create a list to store each record regarding each wifi access point
            lastSample = new ArrayList<WifiInfo>();
            //Extract the interesting features from the wifi scan result
            for(int i = 0; i < results.size(); i++) {
                //For each wifi access point spotted
                Integer freq = results.get(i).frequency;
                Integer level = results.get(i).level;
                WifiInfo toAdd = new WifiInfo(results.get(i).BSSID, results.get(i).SSID, freq.toString(), level.toString());
                lastSample.add(toAdd);
            }
            //Finally add the sample inside another list
            finalSamples.add(lastSample);
            iteration++;
            //When it reaches the desired number of samples it stops sampling
            if(iteration == NUM_SAMPLES)
                stopSampling();
        }
    }

    //Private class that implements the async-task needed to send the samples to the server
    private class AsyncQuery extends AsyncTask<List<List<WifiInfo>>,Void, String>{
        private SocketClient socket;

        @Override
        protected String doInBackground(List<List<WifiInfo>>... params) {
            //Create the socket
            socket = new SocketClient(ServerAddress, ServerPort, timeOutInterval);
            List<List<WifiInfo>> toSend = params[0];
            if(socket == null) {
                Log.e(TAG, "Problems creating the socket");
                return null;
            }
            //Connect the socket to the server
            if(!socket.SocketConnect()){
                Log.e(TAG, "Problems in connecting to the server");
                return null;
            }
            //For each single sample: take it and send it to the server
            for(List<WifiInfo> sample : toSend) {
                System.out.println(sample.toString());
                if (!socket.sendQuery(sample)) {
                    Log.e(TAG, "Problems in sending the query to the server");
                    return null;
                }
            }
            //Read the server response
            String response = socket.readLine();
            socket.closeSocket();
            if(response == null) {
                Log.e(TAG, "Problems in retrieving the response from the server");
                return null;
            }
            return response;
        }

        @Override
        protected void onPostExecute(String s) {            //This function is called when the function "doInBackground" is finished
            //Remove the waiting dialog and clear the samples data structure
            progress.dismiss();
            finalSamples.clear();
            //Show the result to the user
            showResponse(s);
            if(s == null){
                Toast.makeText(Locator.this, "Server timeout, try later", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        //Function that parses the response of the server and shows it to the user
        private void showResponse(String s) {
            if(s == null){
                TextView toShow = (TextView) findViewById(R.id.text_response);
                toShow.setText("");
                return;
            }
            //Parse the response and build the string to be shown
            String toSet;
            String[] splitted = s.split("_");
            String[] confidence = splitted[2].split(" ");
            toSet = "You are in " + splitted[0] + " at floor " + splitted[1] + " inside room " + confidence[0];
            toSet += " with " + confidence[1] + " probability";
            TextView toShow = (TextView) findViewById(R.id.text_response);
            toShow.setText(toSet);

        }
    }

}
