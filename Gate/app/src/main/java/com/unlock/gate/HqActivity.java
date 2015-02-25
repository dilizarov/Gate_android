package com.unlock.gate;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.gc.materialdesign.views.ButtonFloat;
import com.unlock.gate.adapters.KeysListAdapter;
import com.unlock.gate.models.Gate;
import com.unlock.gate.models.Key;
import com.unlock.gate.utils.APIRequestManager;
import com.unlock.gate.utils.ActionBarListActivity;
import com.unlock.gate.utils.Butter;
import com.unlock.gate.utils.VolleyErrorHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class HqActivity extends ActionBarListActivity {

    private ListView keysList;
    private TextView noKeysMessage;
    private ButtonFloat createKey;

    private ArrayList<Key> keys;
    private ArrayList<Key> adapterKeys;
    private KeysListAdapter listAdapter;

    private MenuItem refreshButton;

    private ArrayList<Gate> gates;

    private LinearLayout progressBarHolder;

    private final int UPDATE_GATES_INTENT = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hq);

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#000000")));

        instantiateViews();

        gates = getIntent().getParcelableArrayListExtra("gates");

        keys = new ArrayList<Key>();
        adapterKeys = new ArrayList<Key>();

        setCreateKeyClickListener();
        setListItemClickListeners();

        if (savedInstanceState != null) {
            keys = savedInstanceState.getParcelableArrayList("keys");
            adaptNewKeysToList();
            progressBarHolder.setVisibility(View.GONE);
        } else {
            requestKeysAndPopulateListView(false);
        }
    }

    private void instantiateViews() {
        keysList = getListView();
        noKeysMessage = (TextView) findViewById(R.id.noKeysMessage);
        createKey = (ButtonFloat) findViewById(R.id.createKey);

        progressBarHolder = (LinearLayout) findViewById(R.id.keysProgressBarHolder);

    }

    private void adaptNewKeysToList() {

        noKeysMessage.setText(R.string.no_keys_default);
        noKeysMessage.setVisibility(
                keys.size() == 0
                ? View.VISIBLE
                : View.GONE
        );

        if (listAdapter == null) {
            listAdapter = new KeysListAdapter(HqActivity.this, adapterKeys);
            keysList.setAdapter(listAdapter);
        }

        adapterKeys.clear();
        adapterKeys.addAll(keys);
        listAdapter.notifyDataSetChanged();
    }

    private void setCreateKeyClickListener() {
        createKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (gates.size() == 0) {
                    Butter.between(HqActivity.this, "You have no Gates to unlock");
                } else {
                    Intent intent = new Intent(HqActivity.this, UnlockGateActivity.class);
                    intent.putParcelableArrayListExtra("gates", gates);
                    startActivityForResult(intent, UPDATE_GATES_INTENT);
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case UPDATE_GATES_INTENT:
                if (resultCode == RESULT_OK) {

                    final ArrayList<Key> keysToAdd = data.getParcelableArrayListExtra("keys");

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            int len = keysToAdd.size();
                            for (int i = 0; i < len; i++) {
                                keys.add(keysToAdd.get(i));
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adaptNewKeysToList();
                                }
                            });
                        }
                    }).start();

                }
                break;
        }
    }

    private void setListItemClickListeners() {

        keysList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Key key;

                if (position >= 0 && position < keys.size()) {
                    key = keys.get(position);
                } else {
                    return;
                }

                new MaterialDialog.Builder(HqActivity.this)
                        .title("Key")
                        .content(Html.fromHtml("<b>" + key.getKey() + "</b> unlocks " + key.gatesList()))
                        .autoDismiss(false)
                        .positiveText("SHARE")
                        .negativeText("CANCEL")
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                String extraText = "Use " + key.getKey() + " to #unlock " + key.gatesList() + " on #Gate\n\nhttp://unlockgate.today";
                                String extraSubject = "Unlock Gates with this Key";

                                Intent emailIntent = new Intent();
                                emailIntent.setAction(Intent.ACTION_SEND);
                                emailIntent.putExtra(Intent.EXTRA_TEXT, extraText);
                                emailIntent.putExtra(Intent.EXTRA_SUBJECT, extraSubject);
                                emailIntent.setType("message/rfc822");

                                PackageManager pm = getPackageManager();
                                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                                sendIntent.setType("text/plain");

                                Intent openInChooser = Intent.createChooser(emailIntent, "Share your key using...");

                                List<ResolveInfo> resInfo = pm.queryIntentActivities(sendIntent, 0);
                                List<LabeledIntent> intentList = new ArrayList<LabeledIntent>();

                                for (int i = 0; i < resInfo.size(); i++) {
                                    ResolveInfo ri = resInfo.get(i);
                                    String packageName = ri.activityInfo.packageName;

                                    if (packageName.contains("android.email")) {
                                        emailIntent.setPackage(packageName);
                                    } else if (packageName.contains("twitter") ||
                                            packageName.contains("mms") ||
                                            packageName.contains("sms") ||
                                            packageName.contains("android.gm") ||
                                            packageName.contains("skype") ||
                                            packageName.contains("tumblr")) {

                                        Intent intent = new Intent();
                                        intent.setComponent(new ComponentName(packageName, ri.activityInfo.name));
                                        intent.setAction(Intent.ACTION_SEND);
                                        intent.setType("text/plain");
                                        intent.putExtra(Intent.EXTRA_SUBJECT, extraSubject);

                                        if (packageName.contains("mms") || packageName.contains("sms"))
                                            intent.putExtra(Intent.EXTRA_TEXT, key.getKey());
                                        else intent.putExtra(Intent.EXTRA_TEXT, extraText);

                                        intentList.add(new LabeledIntent(intent, packageName, ri.loadLabel(pm), ri.icon));
                                    }
                                }

                                LabeledIntent[] extraIntents = intentList.toArray(new LabeledIntent[intentList.size()]);
                                openInChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents);
                                startActivity(openInChooser);
                            }

                            @Override
                            public void onNegative(MaterialDialog dialog) {
                                dialog.dismiss();
                            }
                        }).show();
            }

        });

        keysList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                                           long id) {

                final int keyIndex = position;

                // As Gate grows, we'll add more to this that could be done.
                final CharSequence[] items = {
                        "Delete"
                };

                new MaterialDialog.Builder(HqActivity.this)
                        .title(keys.get(keyIndex).getKey() + " for " + keys.get(keyIndex).gatesList())
                        .items(items)
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                switch (which) {
                                    case 0:
                                        new MaterialDialog.Builder(HqActivity.this)
                                                .title(keys.get(keyIndex).getKey())
                                                .content(R.string.confirm_delete_key_message)
                                                .positiveText(R.string.yes_caps)
                                                .negativeText(R.string.no_caps)
                                                .callback(new MaterialDialog.ButtonCallback() {
                                                    @Override
                                                    public void onPositive(MaterialDialog dialog) {
                                                        dialog.dismiss();

                                                        deleteKey(keys.get(keyIndex), keyIndex);
                                                    }

                                                    @Override
                                                    public void onNegative(MaterialDialog dialog) {
                                                        dialog.dismiss();
                                                    }
                                                }).show();
                                        break;
                                }
                            }
                        }).show();

                return true;
            }
        });
    }

    private void requestKeysAndPopulateListView(final boolean refreshing) {

        JSONObject params = new JSONObject();

        Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                final JSONObject jsonResponse = response;

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        keys.clear();

                        JSONArray jsonKeys = jsonResponse.optJSONArray("keys");
                        int len = jsonKeys.length();
                        for (int i = 0; i < len; i++) {
                            JSONObject jsonKey = jsonKeys.optJSONObject(i);

                            ArrayList<String> gateNames = new ArrayList<String>();
                            JSONArray jsonGates = jsonKey.optJSONArray("gates");
                            for (int j = 0; j < jsonGates.length(); j++) {
                                gateNames.add(jsonGates.optJSONObject(j).optString("name"));
                            }

                            Key key = new Key(jsonKey.optString("key"),
                                    jsonKey.optString("updated_at"),
                                    gateNames);

                            keys.add(key);

                        }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adaptNewKeysToList();
                            progressBarHolder.setVisibility(View.GONE);

                            if (refreshing && refreshButton != null) {
                                keysList.setSelection(0);
                                refreshButton.setActionView(null);
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
                VolleyErrorHandler volleyError = new VolleyErrorHandler(error);
                progressBarHolder.setVisibility(View.GONE);

                if (keys.size() == 0) {
                    if (volleyError.isConnectionError())
                        noKeysMessage.setText(R.string.volley_no_connection_error);
                    else
                        noKeysMessage.setText(R.string.gate_error_message);

                    noKeysMessage.setVisibility(View.VISIBLE);
                }

                Butter.down(HqActivity.this, volleyError.getMessage());

                if (refreshing && refreshButton != null) {
                    refreshButton.setActionView(null);
                }
            }
        };

        APIRequestManager.getInstance().doRequest().getKeys(params, listener, errorListener);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList("keys", keys);
    }

    private void deleteKey(final Key key, final int index) {
        keys.remove(key);
        adaptNewKeysToList();
        Butter.down(this, "Deleted " + key.getKey());

        JSONObject params = new JSONObject();

        Response.Listener<Integer> listener = new Response.Listener<Integer>() {
            @Override
            public void onResponse(Integer response) {
                //Don't do anything. Eagerly did actions assuming the request succeeds.
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyErrorHandler volleyError = new VolleyErrorHandler(error);

                keys.add(index, key);
                adaptNewKeysToList();

                Butter.down(HqActivity.this, volleyError.getMessage());
            }
        };

        APIRequestManager.getInstance().doRequest().deleteKey(key, params, listener, errorListener);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_hq, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_refresh_keys:
                if (refreshButton == null) refreshButton = item;
                ProgressBar progressBar = new ProgressBar(this);
                progressBar.setIndeterminate(true);
                progressBar.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progress_action_bar));
                refreshButton.setActionView(progressBar);
                requestKeysAndPopulateListView(true);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
