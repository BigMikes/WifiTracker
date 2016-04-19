package com.project.mps.wifitracker;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import static android.widget.Toast.LENGTH_SHORT;

public class Locator extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locator);

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
                break;
            case R.id.contribute_button:
                //Start the new activity for the contribution process
                Intent intent = new Intent(this, Contribution.class);
                startActivity(intent);
                break;
        }
    }
}
