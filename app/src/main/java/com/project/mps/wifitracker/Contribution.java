package com.project.mps.wifitracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.widget.Toast.LENGTH_SHORT;


public class Contribution extends AppCompatActivity implements View.OnClickListener {

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


        //Suggested GUI inputs
        setInputsAdapters(R.id.input_building,R.array.buildings_array);
        //adapter doesn't work with less than one character
        //setInputsAdapters(R.id.input_floor,R.array.floors_array);

        //removed since each building has it's own way to number rooms
        //setInputsAdapters(R.id.input_room,R.array.rooms_array);

        //setInputsAdapters(R.id.input_num_samp,R.array.samples_array);

        //Set the listener for the start measuring button

        //Set on click listener
        Button btStart = (Button) findViewById(R.id.button_start);
        Button btExport = (Button) findViewById(R.id.button_export);
        try {
            assert btStart != null;
            btStart.setOnClickListener(this);
            assert btExport != null;
            btExport.setOnClickListener(this);
        } catch (AssertionError ae) {
            Log.v("ASSERTION_ERROR: ", ae.getMessage());
            Toast.makeText(Contribution.this, "Button not reachable: ", LENGTH_SHORT).show();
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu1, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.export:
                Log.v("MENU: ", "export");
                dbm.LogDb();
                dbm.exportDb();
                return true;
            case R.id.send:
                return true;
            case R.id.emptyDb:
                Log.v("MENU: ", "emptyDb");
                dbm.emptyDb();
                return true;
            case R.id.deleteFile:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void showDialogConfirmation(String building, String floor, String room, String nSamples) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String toShow = "Bulding: " + building +
                "\nFloor: " + floor +
                "\nRoom: " + room +
                "\nNumber of samples: " + nSamples +
                "\nIs it correct?";
        builder.setMessage(toShow);
        builder.setTitle("Confirmation");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                scanWifi();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //Do nothing
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private boolean createMeasurementObject(String building, String floor, String room, String nSamples){
        //Check input values
        if(building.isEmpty() || floor.isEmpty() || room.isEmpty() || nSamples.isEmpty()){
            Toast.makeText(this, "Some fields are empty, please fill them", Toast.LENGTH_LONG).show();
            return false;
        }
        numberOfSamples = Integer.parseInt(nSamples);
        measurement = new Measurement(building.toLowerCase(), floor.toLowerCase(), room.toLowerCase(),null);
        if(measurement == null)
            return false;
        return true;
    }


    private void setInputsAdapters(int autoCompleteText, int strings_array) {
        // Get a reference to the AutoCompleteTextView in the layout
        AutoCompleteTextView autocompleteTextView = (AutoCompleteTextView) findViewById(autoCompleteText);
        // Get the string array
        String[] strings = getResources().getStringArray(strings_array);
        // Create the adapter and set it to the AutoCompleteTextView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, strings);
        assert autocompleteTextView != null;
        autocompleteTextView.setAdapter(adapter);
        autocompleteTextView.setThreshold(1);
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
        mProgress.setMax(numberOfSamples);
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


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_start:
                Log.v("BUTTON HANDLER: ", "start");
                EditText building = (EditText) findViewById(R.id.input_building);
                EditText floor = (EditText) findViewById(R.id.input_floor);
                EditText room = (EditText) findViewById(R.id.input_room);
                EditText nSamples = (EditText) findViewById(R.id.input_num_samp);
                try {
                    assert nSamples != null;
                    assert building != null;
                    assert floor != null;
                    assert room != null;

                    boolean ret = createMeasurementObject(building.getText().toString(), floor.getText().toString(), room.getText().toString(), nSamples.getText().toString());
                    if(ret) {
                        showDialogConfirmation(building.getText().toString(), floor.getText().toString(), room.getText().toString(), nSamples.getText().toString());
                    }
                } catch (AssertionError ae) {
                    Log.v("ASSERTION_ERROR: ", ae.getMessage());
                    Toast.makeText(Contribution.this, "Fill every field", LENGTH_SHORT).show();
                }
                break;
            case R.id.button_export:
                Log.v("BUTTON HANDLER: ", "export");
                dbm.LogDb();
                dbm.exportDb();
                break;
        }
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
                if(!dbm.store(measurement)) {
                    Toast.makeText(Contribution.this, "Impossible to write on db", Toast.LENGTH_SHORT).show();
                }
                measurement.deleteSamples();
                Log.i(TAG, "END");
                if (numberOfSamples == 0) {
                    stopScan();
                }
            }
        }
    }
}
