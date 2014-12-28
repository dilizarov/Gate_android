package com.unlock.gate;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
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
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.astuetz.PagerSlidingTabStrip;
import com.unlock.gate.models.Network;
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
public class MainActivity extends FragmentActivity {

    private final int UPDATE_POST_INTENT = 2;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
    }

    @Override
    public void onResume() {
        super.onResume();

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
                        .setTitle("Please activate NFC")
                        .setMessage("NFC is required to use Gate and does not drain any battery")
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
                // By waiting 100 milliseconds, you basically guarantee that the keyboard is gone
                // and the difference in time honestly isn't even noticeable.
                if (!activateNfcDialog.isShowing() &&
                    !airplaneModeIsOn())
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            activateNfcDialog.show();
                        }
                    }, 100);
            }
        }

        Intent callingIntent = getIntent();
        Bundle extras = callingIntent.getExtras();

        if (extras != null && extras.getBoolean("notification", false)) {
            showFeed(null, true, false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

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
                case 0:  return FeedFragment.newInstance(position);
                case 1:  return NetworksFragment.newInstance(position);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_unlock_gates:
                Intent intent = new Intent(this, UnlockGateActivity.class);
                intent.putParcelableArrayListExtra("networks", getNetworks());
                startActivity(intent);
                return true;
            case R.id.action_offer_advice:
                test_post_bump();
                return true;
            case R.id.action_logout:
                progressDialog = ProgressDialog.show(MainActivity.this, "",
                        getString(R.string.progress_dialog_server_processing_request), false, true);
                logout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
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
            String grantedNetworkIdsString = payload.get(2);

            //grantedNetworkIdsString is "id, id2, id3, id4, id5" where idx is a UUID.
            ArrayList<String> grantedNetworkIds = new ArrayList<String>(Arrays.asList(grantedNetworkIdsString.split(", ")));

            progressDialog = ProgressDialog.show(MainActivity.this, "", "Unlocking gates...", false, true);
            grantAccessToNetworks(grantedNetworkIds, gatekeeperId, gatekeeperName);
        }
    }

    public void grantAccessToNetworks(ArrayList<String> grantedNetworkIds, String gatekeeperId, final String gatekeeperName) {
        try {

            JSONObject params = new JSONObject();
            params.put("user_id", mSessionPreferences.getString(getString(R.string.user_id_key), null))
                  .put("auth_token", mSessionPreferences.getString(getString(R.string.user_auth_token_key), null))
                  .put("network_ids", new JSONArray(grantedNetworkIds));

            Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(final JSONObject response) {
                    progressDialog.dismiss();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            JSONArray jsonNetworks = response.optJSONArray("networks");
                            int len = jsonNetworks.length();

                            final ArrayList<Network> newNetworks = new ArrayList<Network>();
                            final ArrayList<String> networkNames = new ArrayList<String>();

                            for (int i = 0; i < len; i++) {
                                JSONObject jsonNetwork = jsonNetworks.optJSONObject(i);
                                Network network = new Network(jsonNetwork.optString("external_id"),
                                        jsonNetwork.optString("name"),
                                        jsonNetwork.optInt("users_count"),
                                        jsonNetwork.optJSONObject("creator").optString("name"));

                                networkNames.add(network.getName());
                                newNetworks.add(network);
                            }

                            Collections.sort(networkNames, String.CASE_INSENSITIVE_ORDER);
                            Collections.sort(newNetworks, new Comparator<Network>() {
                                public int compare(Network n1, Network n2) {
                                    return n1.getName().compareToIgnoreCase(n2.getName());
                                }
                            });

                            final NetworksFragment networksFragment = (NetworksFragment) adapter.getRegisteredFragment(1);
                            networksFragment.addNetworksToArrayList(newNetworks);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    networksFragment.adaptNewGatesToList();

                                    CharSequence[] items = networkNames.toArray(new CharSequence[networkNames.size()]);
                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                    builder.setTitle(gatekeeperName + " granted you access to these networks...")
                                           .setItems(items, new DialogInterface.OnClickListener() {
                                               @Override
                                               public void onClick(DialogInterface dialog, int which) {
                                                   dialog.dismiss();
                                                   showFeed(newNetworks.get(which), false, true);
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

                    if (volleyError.isExpectedError())
                        Butter.down(MainActivity.this, volleyError.getPrettyErrors());
                    else
                        Butter.down(MainActivity.this, volleyError.getMessage());
                }
            };

            APIRequestManager.getInstance().doRequest().grantAccessToNetworks(gatekeeperId, params, listener, errorListener);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    public void logout() {
        try {

            JSONObject params = new JSONObject();
            params.put("user_id", mSessionPreferences.getString(getString(R.string.user_id_key), null))
                  .put("auth_token", mSessionPreferences.getString(getString(R.string.user_auth_token_key), null));

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

            progressDialog.dismiss();
            SharedPreferences.Editor editor = mSessionPreferences.edit();
            editor.clear().commit();

            Intent intent = new Intent(MainActivity.this, LoginRegisterActivity.class);
            startActivity(intent);
            finish();
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }


    public void createPost() {
        Toast.makeText(this, "Making feed_item", Toast.LENGTH_SHORT).show();
    }

    public void createNetwork() {
        Toast.makeText(this, "Making network", Toast.LENGTH_SHORT).show();
    }

    public void createKey() {
        Toast.makeText(this, "Making key", Toast.LENGTH_SHORT).show();
    }

    public void showFeed(Network network, boolean refresh, boolean smoothScroll) {
        pager.setCurrentItem(0, smoothScroll);

        FeedFragment feedFragment = (FeedFragment) adapter.getRegisteredFragment(0);
        feedFragment.getNetworkFeed(network, refresh);
    }

    public ArrayList<Network> getNetworks() {
        NetworksFragment networksFragment = (NetworksFragment) adapter.getRegisteredFragment(1);
        return networksFragment.getNetworks();
    }

    public void setTitle(Network network) {

        getActionBar().setTitle(
                network == null
                ? "Aggregate"
                : network.getName()
        );
    }

    private void configureNFC() {
        mNfcPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

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
        }
    }

    private void test_post_bump() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                final ArrayList<Network> newNetworks = new ArrayList<Network>();
                final ArrayList<String> networkNames = new ArrayList<String>();

                for (int i = 0; i < 14; i++) {
                    Network network = new Network("wowowowow" + i,
                            "zgme" + i,
                            14,
                            "Squidward" + i);

                    networkNames.add(network.getName());
                    newNetworks.add(network);
                }

                Collections.sort(networkNames, String.CASE_INSENSITIVE_ORDER);
                Collections.sort(newNetworks, new Comparator<Network>() {
                    public int compare(Network n1, Network n2) {
                        return n1.getName().compareToIgnoreCase(n2.getName());
                    }
                });

                final NetworksFragment networksFragment = (NetworksFragment) adapter.getRegisteredFragment(1);
                networksFragment.addNetworksToArrayList(newNetworks);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        networksFragment.adaptNewGatesToList();

                        CharSequence[] items = networkNames.toArray(new CharSequence[networkNames.size()]);
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("Irene Nguyen" + " granted you access to these networks...")
                                .setItems(items, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        Toast.makeText(MainActivity.this, "No", Toast.LENGTH_LONG).show();
                                    }
                                }).create().show();
                    }
                });

            }
        }).start();

    }

    @SuppressWarnings("deprecation")
    public boolean airplaneModeIsOn() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return Settings.System.getInt(this.getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        } else {
            return Settings.Global.getInt(this.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        }
    }

}
