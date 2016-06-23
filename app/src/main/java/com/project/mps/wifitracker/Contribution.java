package com.project.mps.wifitracker;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
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
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.widget.Toast.LENGTH_SHORT;


public class Contribution extends AppCompatActivity implements View.OnClickListener,NumberPicker.OnValueChangeListener {

    private static final String TAG = "CONTRIBUTION";
    private WifiManager WifiManager;
    private WifiReceiver WifiRec;
    private Timer timerTask;
    private boolean measuring;
    private int numberOfSamples;
    private DbManager dbm;
    private Measurement measurement;
    private ProgressBar mProgress;

    private static final String ServerAddress = "192.168.1.19";
    private static final int ServerPort = 8000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contribution);

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
        if(!WifiManager.isWifiEnabled()){
            WifiManager.setWifiEnabled(true);
            Toast.makeText(this, "Activating Wifi module", Toast.LENGTH_LONG).show();
        }


        //Set on click listener
        Button btStart = (Button) findViewById(R.id.button_start);
        EditText building = (EditText) findViewById(R.id.input_building);
        EditText floor = (EditText) findViewById(R.id.input_floor);
        EditText room = (EditText) findViewById(R.id.input_room);
        try {
            assert btStart != null;
            btStart.setOnClickListener(this);

            //Set the listener to show the picker dialog
            assert building != null;
            assert floor != null;
            assert room != null;
            building.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        showDialogPicker("Buildings", v);
                    }
                }
            });
            room.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        showDialogPicker("Rooms", v);
                    }
                }
            });

        } catch (AssertionError ae) {
            Log.v("ASSERTION_ERROR: ", ae.getMessage());
            Toast.makeText(Contribution.this, "Error in loading the GUI", LENGTH_SHORT).show();
        }

        //Set default value for num of samples
        EditText samples = (EditText) findViewById(R.id.input_num_samp);
        samples.setText("5");

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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu1, menu);
        return true;
    }

    private void showDialogPicker(String title, View v) {
        //final String[] prova = {"prova1", "prova2", "prova3", "prova4"};
        final String[] stringsToShow;
        String[] buildingsArray = getResources().getStringArray(R.array.buildings_array);
        final EditText view = (EditText) v;
        final Dialog d = new Dialog(Contribution.this);
        if(title.equals("Buildings"))
            stringsToShow = buildingsArray;
        else{
            //Show only the rooms that belong to the given building
            EditText building = (EditText) findViewById(R.id.input_building);
            String tag = "rooms_";
            String temp = building.getText().toString().toLowerCase();
            temp = temp.replaceAll(" ", "_");
            tag += temp;
            Log.v("TAG", tag);
            int resourceId = getResources().getIdentifier(tag, "array", getPackageName());
            if(resourceId == 0) {
                Toast.makeText(this, "Please select a building before", Toast.LENGTH_LONG).show();
                return;
            }
            else
                stringsToShow = getResources().getStringArray(resourceId);
        }
        d.setTitle(title);
        d.setContentView(R.layout.dialog_picker);
        Button set = (Button) d.findViewById(R.id.Set);
        final NumberPicker np = (NumberPicker) d.findViewById(R.id.numberPicker1);
        np.setMinValue(0);
        np.setMaxValue(stringsToShow.length-1);
        np.setDisplayedValues(stringsToShow);
        np.setWrapSelectorWheel(false);
        np.setOnValueChangedListener(this);
        set.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                view.setText(stringsToShow[np.getValue()]);
                d.dismiss();
            }
        });
        d.show();
    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.export:
                Log.v("MENU: ", "export database");
                //print the db if needed
                //dbm.LogDb();
                dbm.deleteDb();
                dbm.exportDb();
                return true;
            case R.id.send:
                dbm.deleteDb();
                dbm.exportDb();
                sendContributionDB();
                return true;
            case R.id.emptyDb:
                Log.v("MENU: ", "empty database");
                dbm.emptyDb();
                return true;
            case R.id.deleteFile:
                Log.v("MENU: ", "delete database");
                dbm.deleteDb();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean sendContributionDB() {
        String databasePath = dbm.getDbPath();
        new AsyncUpload().execute(databasePath);
        return true;
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



    public void scanWifi(){
        mProgress.setMax(numberOfSamples);
        measuring = true;
        timerTask = new Timer();
        timerTask.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                WifiManager.startScan();
            }
        }, 0, 500);
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

        }
    }

    private class AsyncUpload extends AsyncTask<String, Void, Boolean> {
        private SocketClient socket;
        @Override
        protected Boolean doInBackground(String... params) {
            SocketClient socket = null;
            FileInputStream in = null;
            File currentDB = null;
            File data = null;
            DataOutputStream dOut = null;
            byte[] b = new byte[1024];
            int dim = 0;
            int count = 0;
            try {
                socket = new SocketClient(ServerAddress, ServerPort,5000);
                if (socket == null) {
                    Log.e(TAG, "Problems creating the socket");
                    return false;
                }
                if (!socket.SocketConnect()) {
                    Log.e(TAG, "Problems in connecting to the server");
                    return false;
                }

                data = Environment.getDataDirectory();
                currentDB = new File(data, params[0]);
                in = new FileInputStream(currentDB);
                dOut = socket.getDataOutputStream();
                dim = (int)currentDB.length();
                dOut.writeInt(dim); // write length of the message
                while(dim > 0) {
                    count = in.read(b);
                    dOut.write(b,0,count);           // write the message
                    dim -= count;
                }

                /*
                String response = socket.readLine();
                socket.closeSocket();
                if (response == null) {
                    Log.e(TAG, "Problems in retrieving the response from the server");
                    return null;
                }
                */
            }catch(Exception e){
                Log.e(TAG, e.toString());
            }finally {
                try {
                    if (in != null) {
                        in.close();
                        dOut.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (socket != null) {
                    socket.closeSocket();
                }

            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(result == false)
                Toast.makeText(Contribution.this, "Server timeout, try later", Toast.LENGTH_SHORT).show();
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
