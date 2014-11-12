package com.unlock.gate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

import com.unlock.gate.models.Network;

import java.util.ArrayList;
import java.util.List;


public class CreatePostActivity extends Activity {

    private Network currentNetwork;
    private ArrayList<Network> networks;

    private Button writePost;
    private EditText postBody;
    private Button networkSelection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        final Intent intent = getIntent();
        currentNetwork = (Network) intent.getParcelableExtra("currentNetwork");
        networks       = intent.getParcelableArrayListExtra("networks");

        final List<String> items = new ArrayList<String>();
        for ( Network network : networks) items.add(network.getName());

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, items);

        networkSelection = (Button) findViewById(R.id.networkSelection);
        if (currentNetwork != null) networkSelection.setText(currentNetwork.getName());

        networkSelection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(CreatePostActivity.this)
                               .setAdapter(adapter, new DialogInterface.OnClickListener() {
                                   @Override
                                   public void onClick(DialogInterface dialog, int which) {
                                       currentNetwork = networks.get(which);
                                       networkSelection.setText(items.get(which));

                                       if (currentNetwork != null &&
                                           postBody.getText().toString().length() > 0) writePost.setEnabled(true);
                                       else writePost.setEnabled(false);

                                       dialog.dismiss();
                                   }
                               }).create().show();
            }
        });

        postBody = (EditText) findViewById(R.id.postBody);
        postBody.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0 &&
                    currentNetwork != null) writePost.setEnabled(true);
                else writePost.setEnabled(false);
            }
        });

        writePost = (Button) findViewById(R.id.writePost);
        writePost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent.putExtra("network", currentNetwork);
                intent.putExtra("postBody", postBody.getText().toString());
                setResult(RESULT_OK, intent);

                finish();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_create_post, menu);
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