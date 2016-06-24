package com.project.mps.wifitracker;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LocatorService extends Service {
    private final IBinder servBinder = new LocatorBinder();

    /*-----------------------------CONFIG VARIABLES--------------------------*/
    private static final String ServerAddress = "192.168.1.19";
    //private static final String ServerAddress = "ciaoasdfghjkl.ddns.net";
    private static final int ServerPort = 8080;
    private static final String TAG = "ServiceLocator";
    private int NUM_SAMPLES = 3;
    private int timeOutInterval = 10000;

    /*----------------------------STATE VARIABLES----------------------------*/
    private android.net.wifi.WifiManager WifiManager;
    private WifiReceiver WifiRec;
    private List<WifiInfo> lastSample;
    private List<List<WifiInfo>> finalSamples;
    private boolean onQuerying;
    private int iteration;
    private Timer timerTask;
    private Handler handler;

    public LocatorService() {
    }

    @Override
    public void onCreate() {
        WifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        WifiRec = new WifiReceiver();
        onQuerying = false;
        finalSamples = new ArrayList<List<WifiInfo>>(NUM_SAMPLES);
        iteration = 0;
        //Create a thread handler to receive and process the intents sent by the system
        //It is necessary to have another thread because we need synchronization
        HandlerThread handlerThread = new HandlerThread("Wifi scan receiver handler");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        handler = new Handler(looper);
        //Register the intent receiver
        registerReceiver(WifiRec, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION), null, handler);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        try {   //Since there is no way to know if or not the receiver is registered
            unregisterReceiver(WifiRec);
        }catch(IllegalArgumentException e){

        }
        return super.onUnbind(intent);
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        try {   //Since there is no way to know if or not the receiver is registered
            unregisterReceiver(WifiRec);
        }catch(IllegalArgumentException e){

        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return servBinder;
    }

    public class LocatorBinder extends Binder {
        LocatorService getService(){
            return LocatorService.this;
        }
    }

    /**
     * The actual function for starting the localization process.
     * It is the one that will be called by other application
     * @return String. The received position with this format: building_floor_room confidence
     */
    public String localizeMe(){
        String result = null;
        Log.d(TAG, "Start sampling");
        //Start the sampling of wifi scans
        startSampling();
        try {
            //There will be a separated thread that fill up this data structure with wifi scans
            synchronized (finalSamples) {
                Log.d(TAG, "Wait until all samples are collected");
                //Instead this thread has to wait until all samples are collected.
                //It will be notified by the other thread
                while (finalSamples.size() != NUM_SAMPLES)
                    finalSamples.wait();
            }
            Log.d(TAG, "Samples have been collected");
            Log.d(TAG, "Wait the server's response");
            //With the collected samples, it asks to the server for classification
            result = queryToServer(finalSamples);
            if(result.contains("Not found")){
                Log.d(TAG, "The server wasn't able to find my location");
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        finalSamples.clear();
        return result;
    }

    //It initializes the sampling process
    private void startSampling(){
        //Before start we need to register the broadcast receiver to acquire the intents
        registerReceiver(WifiRec, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION), null, handler);
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
        //Notify and wake up the main thread that the samples have been collected
        synchronized (finalSamples) {
            finalSamples.notifyAll();
        }
        //At the end we unregister the broadcast receiver
        unregisterReceiver(WifiRec);
    }

    private String queryToServer(List<List<WifiInfo>> toSend){
        SocketClient socket;
        //Create the socket
        socket = new SocketClient(ServerAddress, ServerPort, timeOutInterval);
        Log.d(TAG, "Created the socket");
        if(socket == null) {
            Log.e(TAG, "Problems creating the socket");
            return null;
        }
        //Connect the socket to the server
        if(!socket.SocketConnect()){
            Log.e(TAG, "Problems in connecting to the server");
            return null;
        }
        Log.d(TAG, "Connected to the server");
        //For each single sample: take it and send it to the server
        for(List<WifiInfo> sample : toSend) {
            System.out.println(sample.toString());
            if (!socket.sendQuery(sample)) {
                Log.e(TAG, "Problems in sending the query to the server");
                return null;
            }
        }
        Log.d(TAG, "Samples have been sent");
        //Read the server response
        String response = socket.readLine();
        Log.d(TAG, "Response received");
        socket.closeSocket();
        if(response == null) {
            Log.e(TAG, "Problems in retrieving the response from the server");
            return null;
        }
        return response;
    }



    //Private class that implements the broadcast receiver of wifi scan results intent
    class WifiReceiver extends BroadcastReceiver {
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
            synchronized (finalSamples) {
                finalSamples.add(lastSample);
            }
            iteration++;
            //When it reaches the desired number of samples it stops sampling
            if(iteration == NUM_SAMPLES)
                stopSampling();
        }
    }

}
