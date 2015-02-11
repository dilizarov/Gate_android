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
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.unlock.gate.models.Gate;
import com.unlock.gate.utils.APIRequestManager;
import com.unlock.gate.utils.Butter;
import com.unlock.gate.utils.CustomEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class CreatePostActivity extends Activity {

    private Gate currentGate;
    private ArrayList<Gate> gates;

    private ImageButton writePost;
    private CustomEditText postBody;
    private TextView gateSelection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        final Intent intent = getIntent();

        if (savedInstanceState != null) {
            currentGate = savedInstanceState.getParcelable("currentGate");
            gates       = savedInstanceState.getParcelableArrayList("gates");
        } else {
            currentGate = intent.getParcelableExtra("currentGate");
            gates       = intent.getParcelableArrayListExtra("gates");
        }

        gateSelection = (TextView) findViewById(R.id.gateSelection);
        writePost        = (ImageButton) findViewById(R.id.writePost);
        postBody         = (CustomEditText) findViewById(R.id.postBody);
        postBody.requestFocus();

        if (!(postBody.getText().toString().trim().length() > 0))
            writePost.setEnabled(false);

        postBody.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().length() > 0 &&
                    currentGate != null) writePost.setEnabled(true);
                else writePost.setEnabled(false);
            }
        });

        writePost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent.putExtra("gate", currentGate);
                intent.putExtra("postBody", postBody.getText().toString().trim());
                setResult(RESULT_OK, intent);

                finish();
            }
        });

        if (intent.getStringExtra("postBody") != null) postBody.append(intent.getStringExtra("postBody"));

        if (intent.getStringExtra("errorMessage") != null) {
            Butter.between(this, intent.getStringExtra("errorMessage"));
        }

        gateSelection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (gates.isEmpty()) {
                    gateSelection.setText("Loading Gates...");
                    gateSelection.setEnabled(false);

                    loadGates(true);
                } else {
                    final List<String> items = new ArrayList<String>();
                    for (Gate gate : gates) items.add(gate.getName());

                    final ArrayAdapter<String> adapter = new ArrayAdapter<String>(CreatePostActivity.this,
                            android.R.layout.simple_spinner_dropdown_item, items);

                    new AlertDialog.Builder(CreatePostActivity.this)
                            .setTitle("Select a Gate")
                            .setAdapter(adapter, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    currentGate = gates.get(which);
                                    gateSelection.setText(items.get(which));

                                    if (currentGate != null &&
                                            postBody.getText().toString().trim().length() > 0) writePost.setEnabled(true);
                                    else writePost.setEnabled(false);

                                    dialog.dismiss();
                                }
                            }).create().show();
                }

            }
        });

        if (gates.isEmpty()) {
            gateSelection.setText("Loading Gates...");
            gateSelection.setEnabled(false);

            loadGates(false);
        } else {
            if (currentGate != null) gateSelection.setText(currentGate.getName());
            else {
                gateSelection.performClick();
            }
        }
    }

    private void loadGates(final Boolean forceSelectGate) {
        JSONObject params = new JSONObject();

        Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(final JSONObject response) {

                new Thread(new Runnable() {

                    public void run() {
                        gates.clear();

                        JSONArray jsonGates = response.optJSONArray("gates");
                        int len = jsonGates.length();
                        for (int i = 0; i < len; i++) {
                            JSONObject jsonGate = jsonGates.optJSONObject(i);
                            Gate gate = new Gate(jsonGate.optString("external_id"),
                                    jsonGate.optString("name"),
                                    jsonGate.optInt("users_count"),
                                    jsonGate.optJSONObject("creator").optString("name"));

                            gates.add(gate);
                        }

                        runOnUiThread(new Runnable() {
                            public void run() {
                                if (gates.isEmpty()) {
                                    Butter.between(CreatePostActivity.this, "No Gates Unlocked\nUnlock a Gate so you can post");

                                    setResult(RESULT_CANCELED, getIntent());
                                    finish();
                                } else {
                                    gateSelection.setText("Select a Gate");
                                    gateSelection.setEnabled(true);

                                    if (forceSelectGate) {
                                        gateSelection.performClick();
                                    }
                                }
                            }
                        });
                    }

                }).start();
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Butter.between(CreatePostActivity.this, "We couldn't load your Gates");

                gateSelection.setText("Reload Gates");
                gateSelection.setEnabled(true);
            }
        };

        APIRequestManager.getInstance().doRequest().getGates(params, listener, errorListener);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_create_post, menu);
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("currentGate", currentGate);
        outState.putParcelableArrayList("gates", gates);
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
