package com.unlock.gate;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.astuetz.PagerSlidingTabStrip;
import com.unlock.gate.models.Gate;
import com.unlock.gate.utils.APIRequestManager;
import com.unlock.gate.utils.Butter;
import com.unlock.gate.utils.NfcUtils;
import com.unlock.gate.utils.VolleyErrorHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by davidilizarov on 10/20/14.
 */
public class MainActivity extends ActionBarActivity {

    private final int UPDATE_POST_INTENT = 2;

    private int beforeLength;
    private TextWatcher keyTextWatcher;

    private PagerSlidingTabStrip tabs;
    private ViewPager pager;
    private MyPagerAdapter adapter;
    private SharedPreferences mSessionPreferences;

    private NfcAdapter mNfcAdapter;
    private PendingIntent mNfcPendingIntent;
    private IntentFilter[] mNdefExchangeFilters;
    private boolean mWriteMode = false;
    private ProgressDialog progressDialog;

    // This will keep tabs on NFC and fire off when it is turned on/off/etc.
    // Made for the case when one turns NFC on/off through status bar.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)) {
                if (mNfcAdapter != null) {
                    if (mNfcAdapter.isEnabled()) {
                        configureNFC();
                        mNfcAdapter.enableForegroundDispatch(MainActivity.this, mNfcPendingIntent,
                                mNdefExchangeFilters, null);
                    } else {
                        mNfcAdapter.disableForegroundDispatch(MainActivity.this);
                    }
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#000000")));

        tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        pager = (ViewPager) findViewById(R.id.pager);
        adapter = new MyPagerAdapter(getSupportFragmentManager());

        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(1);

        tabs.setBackgroundColor(Color.BLACK);
        tabs.setUnderlineColor(Color.BLACK);
        tabs.setIndicatorHeight(8);
        tabs.setUnderlineHeight(6);

        tabs.setShouldExpand(true);
        tabs.setViewPager(pager);

        mSessionPreferences = getSharedPreferences(
                getString(R.string.session_shared_preferences_key), MODE_PRIVATE);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        configureNFC();
    }

    @Override
    public void onResume() {
        super.onResume();
        configureNFCDispatch();

        if (mNfcAdapter != null) {
            IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
            this.registerReceiver(mReceiver, filter);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mNfcAdapter != null) {
            this.unregisterReceiver(mReceiver);

            if (mNfcAdapter.isEnabled()) mNfcAdapter.disableForegroundDispatch(this);
        }
    }

    public class MyPagerAdapter extends FragmentStatePagerAdapter {
        SparseArray<Fragment> registeredFragments = new SparseArray<Fragment>();

        private final String[] TITLES = getResources().getStringArray(R.array.tabs);

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }

        @Override
        public Fragment getItem(int position) {
            switch(position) {
                case 0:  return FeedFragment.newInstance();
                case 1:  return GatesFragment.newInstance();
            }

            return null;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            registeredFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            registeredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public Fragment getRegisteredFragment(int position) {
            return registeredFragments.get(position);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (!mWriteMode && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {

            NdefMessage[] messages = NfcUtils.getNdefMessages(intent);
            ArrayList<String> payload = NfcUtils.getNdefMessagePayload(messages[0]);

            final String gatekeeperId      = payload.get(0);
            final String gatekeeperName    = payload.get(1);
            String grantedGateIdsString    = payload.get(2);

            //grantedGateIdsString is "id, id2, id3, id4, id5" where idx is a UUID.
            ArrayList<String> grantedGateIds = new ArrayList<String>(Arrays.asList(grantedGateIdsString.split(", ")));

            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(getString(R.string.unlocking_gates));
            progressDialog.setIndeterminate(true);
            progressDialog.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progress));
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();

            grantAccessToGates(grantedGateIds, gatekeeperId, gatekeeperName);
        } else if (intent.getBooleanExtra("mainActivityNotification", false)) {
            showFeed(null, false);
        } else if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())) {
            String post = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (post != null) {
                FeedFragment feedFragment = (FeedFragment) adapter.getRegisteredFragment(0);
                feedFragment.openSharePost(post);
            }
        }
    }

    public void grantAccessToGates(ArrayList<String> grantedGateIds, String gatekeeperId, final String gatekeeperName) {
        try {

            JSONObject params = new JSONObject();
            params.put("gate_ids", new JSONArray(grantedGateIds));

            Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(final JSONObject response) {
                    progressDialog.dismiss();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            JSONArray jsonGates = response.optJSONArray("gates");
                            int len = jsonGates.length();

                            final ArrayList<Gate> newGates = new ArrayList<Gate>();

                            for (int i = 0; i < len; i++) {
                                JSONObject jsonGate = jsonGates.optJSONObject(i);
                                Gate gate = new Gate(jsonGate.optString("external_id"),
                                        jsonGate.optString("name"),
                                        jsonGate.optInt("users_count"),
                                        jsonGate.optJSONObject("creator").optString("name"));

                                newGates.add(gate);
                            }

                            Collections.sort(newGates, new Comparator<Gate>() {
                                public int compare(Gate n1, Gate n2) {
                                    return n1.getName().compareToIgnoreCase(n2.getName());
                                }
                            });


                            // Required for use as CharSequence[] items later for list of Strings.
                            final ArrayList<String> gateNames = new ArrayList<String>();
                            for (Gate gate : newGates) gateNames.add(gate.getName());

                            final GatesFragment gatesFragment = (GatesFragment) adapter.getRegisteredFragment(1);
                            gatesFragment.addGatesToArrayList(newGates);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    gatesFragment.adaptNewGatesToList();

                                    CharSequence[] items = gateNames.toArray(new CharSequence[gateNames.size()]);
                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                    builder.setTitle(gatekeeperName + " granted you access to these Gates...")
                                           .setItems(items, new DialogInterface.OnClickListener() {
                                               @Override
                                               public void onClick(DialogInterface dialog, int which) {
                                                   dialog.dismiss();
                                                   showFeed(newGates.get(which), true);
                                               }
                                           }).create().show();
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
                    progressDialog.dismiss();

                    Butter.down(MainActivity.this, volleyError.getMessage());
                }
            };

            APIRequestManager.getInstance().doRequest().grantAccessToGates(gatekeeperId, params, listener, errorListener);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    public void processKey(String key) {

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.unlocking_gates));
        progressDialog.setIndeterminate(true);
        progressDialog.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progress));
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        JSONObject params = new JSONObject();

            Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {

                @Override
                public void onResponse(final JSONObject response) {
                    progressDialog.dismiss();

                    if (response.optJSONArray("gates").length() == 0) {
                        Butter.down(MainActivity.this, "All gates already unlocked");

                        return;
                    }

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            JSONArray jsonGates = response.optJSONArray("gates");
                            int len = jsonGates.length();

                            final ArrayList<Gate> newGates = new ArrayList<Gate>();

                            for (int i = 0; i < len; i++) {
                                JSONObject jsonGate = jsonGates.optJSONObject(i);
                                Gate gate = new Gate(jsonGate.optString("external_id"),
                                        jsonGate.optString("name"),
                                        jsonGate.optInt("users_count"),
                                        jsonGate.optJSONObject("creator").optString("name"));

                                newGates.add(gate);
                            }

                            Collections.sort(newGates, new Comparator<Gate>() {
                                public int compare(Gate n1, Gate n2) {
                                    return n1.getName().compareToIgnoreCase(n2.getName());
                                }
                            });


                            // Required for use as CharSequence[] items later for list of Strings.
                            final ArrayList<String> gateNames = new ArrayList<String>();
                            for (Gate gate : newGates) gateNames.add(gate.getName());

                            final GatesFragment gatesFragment = (GatesFragment) adapter.getRegisteredFragment(1);
                            gatesFragment.addGatesToArrayList(newGates);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    gatesFragment.adaptNewGatesToList();

                                    CharSequence[] items = gateNames.toArray(new CharSequence[gateNames.size()]);

                                    new MaterialDialog.Builder(MainActivity.this)
                                            .title(response.optJSONObject("meta").optJSONObject("data").optString("gatekeeper") + " granted you access to these Gates...")
                                            .items(items)
                                            .itemsCallback(new MaterialDialog.ListCallback() {
                                                @Override
                                                public void onSelection(MaterialDialog materialDialog, View view, int which, CharSequence charSequence) {
                                                    materialDialog.dismiss();
                                                    showFeed(newGates.get(which), true);
                                                }
                                            }).show();
                                }
                            });
                        }
                    }).start();
                }
            };

            Response.ErrorListener errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    progressDialog.dismiss();

                    VolleyErrorHandler volleyError = new VolleyErrorHandler(error);

                    Butter.down(MainActivity.this, volleyError.getMessage());
                }
            };

            APIRequestManager.getInstance().doRequest().processKey(key, params, listener, errorListener);
    }

    public void logout() {
        try {

            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(getString(R.string.progress_dialog_server_processing_request));
            progressDialog.setIndeterminate(true);
            progressDialog.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progress));
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();

            JSONObject params = new JSONObject();

            JSONObject device = new JSONObject();
            device.put("token", getSharedPreferences(getString(R.string.gcm_preferences_key), MODE_PRIVATE)
                                    .getString("registration_id", ""));

            params.put("device", device);

            Response.Listener<Integer> listener = new Response.Listener<Integer>() {
                @Override
                public void onResponse(final Integer statusCode) {
                    progressDialog.dismiss();

                    SharedPreferences.Editor editor = mSessionPreferences.edit();
                    editor.clear().apply();

                    Intent intent = new Intent(MainActivity.this, LoginRegisterActivity.class);
                    startActivity(intent);
                    finish();


                }
            };

            Response.ErrorListener errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("WHAT IS GOING ON", error.toString());

                    VolleyErrorHandler volleyError = new VolleyErrorHandler(error);
                    progressDialog.dismiss();

                    Butter.down(MainActivity.this, volleyError.getMessage());
                }
            };

            APIRequestManager.getInstance().doRequest().logout(params, listener, errorListener);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }


    public void showFeed(Gate gate, boolean smoothScroll) {
        pager.setCurrentItem(0, smoothScroll);

        FeedFragment feedFragment = (FeedFragment) adapter.getRegisteredFragment(0);
        feedFragment.getGateFeed(gate);
    }

    public ArrayList<Gate> getGates() {
        GatesFragment gatesFragment = (GatesFragment) adapter.getRegisteredFragment(1);
        return gatesFragment.getGates();
    }

    public void setTitle(Gate gate) {
        getSupportActionBar().setTitle(
                gate == null
                ? "Aggregate"
                : gate.getName()
        );
    }

    private void configureNFC() {
        if (mNfcAdapter != null) {
            mNfcPendingIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

            IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);

            try {
                ndefDetected.addDataType("text/plain");
            } catch (IntentFilter.MalformedMimeTypeException e) {
                e.printStackTrace();
            }

            mNdefExchangeFilters = new IntentFilter[] { ndefDetected };
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case UPDATE_POST_INTENT:
                FeedFragment feedFragment = (FeedFragment) adapter.getRegisteredFragment(0);
                feedFragment.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void configureNFCDispatch() {
        if (mNfcAdapter != null && mNfcAdapter.isEnabled()) {
                configureNFC();
                mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
                        mNdefExchangeFilters, null);
        }
    }

    private void showEnterKeyDialog() {
        final MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(R.string.title_dialog_enter_key)
                .customView(R.layout.dialog_enter_key, true).build();

        final EditText keyInput = (EditText) dialog.getCustomView().findViewById(R.id.keyInput);

        beforeLength = 0;

        keyTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                beforeLength = s.length();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(final Editable s) {
                String plausableKey = s.toString().replaceAll("-", "");

                if (plausableKey.length() == 16) {
                    dialog.dismiss();
                    processKey(plausableKey);
                } else if ((s.length() >= beforeLength) && (s.length() == 4 || s.length() == 9 || s.length() == 14)) {
                    keyInput.removeTextChangedListener(keyTextWatcher);
                    keyInput.setText(s.toString() + "-");
                    keyInput.post(new Runnable() {
                        @Override
                        public void run() {
                            keyInput.setSelection(s.length() + 1);
                        }
                    });

                    keyInput.addTextChangedListener(keyTextWatcher);
                } else if ( s.length() < beforeLength && (s.length() == 4 || s.length() == 9 || s.length() == 14)) {
                    keyInput.removeTextChangedListener(keyTextWatcher);
                    keyInput.setText(s.subSequence(0, s.length() - 1));
                    keyInput.post(new Runnable() {
                        @Override
                        public void run() {
                            keyInput.setSelection(s.length() - 1);
                        }
                    });

                    keyInput.addTextChangedListener(keyTextWatcher);
                }
            }
        };

        keyInput.addTextChangedListener(keyTextWatcher);

        dialog.show();
    }

    private void showLogoutDialog() {
        SharedPreferences sessionPreferences = getSharedPreferences(getString(R.string.session_shared_preferences_key),
                Context.MODE_PRIVATE);

        String userName = sessionPreferences.getString(getString(R.string.user_name_key), "");

        new MaterialDialog.Builder(this)
                .title(userName)
                .content("Are you sure you want to log out of Gate?")
                .positiveText(R.string.yes_caps)
                .negativeText(R.string.no_caps)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        dialog.dismiss();

                        logout();
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        dialog.dismiss();
                    }
                }).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_unlock_gates:
                if (getGates().size() == 0) {
                    pager.setCurrentItem(1, true);

                    Butter.between(this, "You have no Gates to unlock");
                } else {
                    Intent intent = new Intent(this, UnlockGateActivity.class);
                    intent.putParcelableArrayListExtra("gates", getGates());
                    startActivity(intent);
                }

                return true;
            case R.id.action_logout:
                showLogoutDialog();
                return true;
            case R.id.enter_key:
                showEnterKeyDialog();
            default:
                return super.onOptionsItemSelected(item);
        }
    }


}
