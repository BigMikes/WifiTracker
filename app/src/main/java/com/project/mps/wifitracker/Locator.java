package com.project.mps.wifitracker;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import static android.widget.Toast.LENGTH_SHORT;

public class Locator extends AppCompatActivity implements View.OnClickListener{
    /*-----------------------------CONFIG VARIABLES--------------------------*/
    private static final String TAG = "ClientLocator";

    /*----------------------------STATE VARIABLES----------------------------*/
    private ProgressDialog progress;
    private android.net.wifi.WifiManager WifiManager;
    private LocatorService locService;
    private boolean connected;

    //Anonymous class to connect with the localization service
    public ServiceConnection connection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocatorService.LocatorBinder binder = (LocatorService.LocatorBinder) service;
            locService = binder.getService();
            Log.i(TAG, "Service retrieved");
            connected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            connected = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locator);
        //If the wifi is turned off, it will be activated
        WifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        if(!WifiManager.isWifiEnabled()){
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
    protected void onStart() {
        super.onStart();
        //When the application starts, we need to create the service and bind to it
        Intent locatorServiceInt = new Intent(this, LocatorService.class);
        bindService(locatorServiceInt,connection,Context.BIND_AUTO_CREATE);
        Log.i(TAG, "Service created");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(connected){
            unbindService(connection);
            connected = false;
        }
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
                    getLocation();
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
    private void getLocation(){
        TextView toShow = (TextView) findViewById(R.id.text_response);
        toShow.setText("Scanning...");
        new getLocationFromService().execute();
    }

    //After it's collected N samples it stops sampling and shows the response
    private void LocationRetrieved(String s){
        progress.dismiss();
        showResponse(s);
    }

    //Function that parses the response of the server and shows it to the user
    private void showResponse(String s) {
        if(s == null){
            TextView toShow = (TextView) findViewById(R.id.text_response);
            toShow.setText("");
            Toast.makeText(this, "The server seems to be offline, please retry later", Toast.LENGTH_SHORT).show();
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

    //To use the localization service it necessary to create and execute an asynctask,
    //or at least to call the function provided by the service in a different thread
    private class getLocationFromService extends AsyncTask<Void,Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            String location = locService.localizeMe();
            return location;
        }

        @Override
        protected void onPostExecute(String s) {
            LocationRetrieved(s);
        }
    }

}
