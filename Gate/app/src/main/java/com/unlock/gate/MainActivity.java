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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.astuetz.PagerSlidingTabStrip;
import com.unlock.gate.models.Gate;
import com.unlock.gate.utils.APIRequestManager;
import com.unlock.gate.utils.Butter;
import com.unlock.gate.utils.NfcUtils;
import com.unlock.gate.utils.VolleyErrorHandler;

import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
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
public class MainActivity extends FragmentActivity {

    private final int UPDATE_POST_INTENT = 2;
    private final int ENTER_KEY_INTENT = 5;

    private int key_attempts;
    private DateTime last_attempt;

    private PagerSlidingTabStrip tabs;
    private ViewPager pager;
    private MyPagerAdapter adapter;
    private SharedPreferences mSessionPreferences;

    private NfcAdapter mNfcAdapter;
    private PendingIntent mNfcPendingIntent;
    private IntentFilter[] mNdefExchangeFilters;
    private boolean mWriteMode = false;
    private ProgressDialog progressDialog;

    private AlertDialog activateNfcDialog;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)) {
                checkNFCEnabled();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setting background of actionbar to black.
        getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#000000")));

        tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        pager = (ViewPager) findViewById(R.id.pager);
        adapter = new MyPagerAdapter(getSupportFragmentManager());

        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(1);

        tabs.setBackgroundColor(getResources().getColor(R.color.white));
        tabs.setIndicatorColor(Color.BLACK);
        tabs.setTextColor(Color.BLACK);
        tabs.setBackgroundColor(Color.WHITE);

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
        checkNFCEnabled();

        // This will keep tabs on NFC and fire off when it is turned on/off/etc.
        if (mNfcAdapter != null) {
            IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
            this.registerReceiver(mReceiver, filter);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        this.unregisterReceiver(mReceiver);

        if (mNfcAdapter != null && mNfcAdapter.isEnabled())
            mNfcAdapter.disableForegroundDispatch(this);
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
            String grantedGateIdsString = payload.get(2);

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
            showFeed(null, true, false);
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
                                                   showFeed(newGates.get(which), false, true);
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
                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                    builder.setTitle(response.optJSONObject("meta").optJSONObject("data").optString("gatekeeper") + " granted you access to these Gates...")
                                            .setItems(items, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                    showFeed(newGates.get(which), false, true);
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
                    progressDialog.dismiss();

                    VolleyErrorHandler volleyError = new VolleyErrorHandler(error);

                    if (volleyError.getStatusCode() == 423) {
                        key_attempts++;

                        if (key_attempts < 5) {
                            Intent intent = new Intent(MainActivity.this, EnterKeyActivity.class);
                            intent.putExtra("errorMessage", volleyError.getMessage());

                            startActivityForResult(intent, ENTER_KEY_INTENT);
                        } else {
                            Butter.down(MainActivity.this, volleyError.getMessage());

                        }
                    } else {
                        Butter.down(MainActivity.this, volleyError.getMessage());
                    }
                }
            };

            APIRequestManager.getInstance().doRequest().processKey(key, params, listener, errorListener);
    }

    public void logout() {
        try {

            JSONObject params = new JSONObject();

            JSONObject device = new JSONObject();
            device.put("token", getSharedPreferences(getString(R.string.gcm_preferences_key), MODE_PRIVATE)
                                    .getString("registration_id", ""));

            params.put("device", device);

            Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(final JSONObject response) {
                }
            };

            Response.ErrorListener errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            };

            APIRequestManager.getInstance().doRequest().logout(params, listener, errorListener);

            SharedPreferences.Editor editor = mSessionPreferences.edit();
            editor.clear().apply();

            Intent intent = new Intent(MainActivity.this, LoginRegisterActivity.class);
            startActivity(intent);
            finish();
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }


    public void showFeed(Gate gate, boolean refresh, boolean smoothScroll) {
        pager.setCurrentItem(0, smoothScroll);

        FeedFragment feedFragment = (FeedFragment) adapter.getRegisteredFragment(0);
        feedFragment.getGateFeed(gate, refresh);
    }

    public ArrayList<Gate> getGates() {
        GatesFragment gatesFragment = (GatesFragment) adapter.getRegisteredFragment(1);
        return gatesFragment.getGates();
    }

    public void setTitle(Gate gate) {
        getActionBar().setTitle(
                gate == null
                ? "Aggregate"
                : gate.getName()
        );
    }

    private void configureNFC() {
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case UPDATE_POST_INTENT:
                FeedFragment feedFragment = (FeedFragment) adapter.getRegisteredFragment(0);
                feedFragment.onActivityResult(requestCode, resultCode, data);
                break;
            case ENTER_KEY_INTENT:
                if (resultCode == RESULT_OK) {
                    final String key = data.getStringExtra("key");

                    processKey(key);
                }

                break;
        }
    }

    private void checkNFCEnabled() {
        if (mNfcAdapter != null) {
            if (mNfcAdapter.isEnabled()) {
                if (activateNfcDialog != null && activateNfcDialog.isShowing())
                    activateNfcDialog.cancel();

                configureNFC();
                mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
                        mNdefExchangeFilters, null);
            } else {
                if (activateNfcDialog == null) {
                    activateNfcDialog = new AlertDialog.Builder(this)
                            .setTitle("Activate NFC")
                            .setMessage("NFC (near field communication) is required to use Gate. NFC does not drain any battery.")
                            .setCancelable(false)
                            .setPositiveButton("Activate", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                        Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
                                        startActivity(intent);
                                    } else {
                                        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                                        startActivity(intent);
                                    }
                                }
                            }).create();
                }

                // I use the handler postDelayed because when someone logs in,
                // sometimes everything happens so fast the Keyboard doesn't have
                // time to leave the view. So in the background, you still see the keyboard.
                // By waiting 100 milliseconds, you basically guarantee that the keyboard is gone.
                if (!activateNfcDialog.isShowing())
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            activateNfcDialog.show();
                        }
                    }, 100);
            }
        }
    }

//    @SuppressWarnings("deprecation")
//    public boolean airplaneModeIsOn() {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
//            return Settings.System.getInt(this.getContentResolver(),
//                    Settings.System.AIRPLANE_MODE_ON, 0) != 0;
//        } else {
//            return Settings.Global.getInt(this.getContentResolver(),
//                    Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
//        }
//    }

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
                logout();
                return true;
            case R.id.enter_key:
                DateTimeComparator comparator = DateTimeComparator.getInstance();
                if (key_attempts < 5 || comparator.compare(last_attempt, DateTime.now().minusMinutes(15)) == -1) {
                    if (key_attempts >= 5) key_attempts = 0;
                    Intent intent = new Intent(this, EnterKeyActivity.class);
                    startActivityForResult(intent, ENTER_KEY_INTENT);
                } else {
                    last_attempt = DateTime.now();
                    Butter.down(this, "Wait 15 minutes for another five attempts");
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }


}
