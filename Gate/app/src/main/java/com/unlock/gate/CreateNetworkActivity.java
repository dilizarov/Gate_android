package com.unlock.gate;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import com.unlock.gate.utils.Butter;


public class CreateNetworkActivity extends Activity {

    private ImageButton createNetwork;
    private EditText createNetworkName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_network);

        final Intent intent = getIntent();

        createNetwork     = (ImageButton) findViewById(R.id.createNetwork);
        createNetworkName = (EditText) findViewById(R.id.createNetworkName);

        if (!(createNetworkName.getText().toString().trim().length() > 0))
            createNetwork.setEnabled(false);


        createNetworkName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().length() > 0) createNetwork.setEnabled(true);
                else createNetwork.setEnabled(false);
            }
        });

        createNetwork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent.putExtra("networkName", createNetworkName.getText().toString().trim());
                setResult(RESULT_OK, intent);

                finish();

            }
        });

        if (intent.getStringExtra("networkName") != null) createNetworkName.append(intent.getStringExtra("networkName"));

        if (intent.getStringExtra("errors") != null) {
            Butter.between(this, intent.getStringExtra("errors"));
        } else if (intent.getStringExtra("errorMessage") != null) {
            Butter.between(this, intent.getStringExtra("errorMessage"));
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_create_network, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
