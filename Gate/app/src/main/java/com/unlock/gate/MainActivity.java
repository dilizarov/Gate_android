package com.unlock.gate;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
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
import com.unlock.gate.utils.NfcUtils;

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

    private PagerSlidingTabStrip tabs;
    private ViewPager pager;
    private MyPagerAdapter adapter;
    private SharedPreferences mSessionPreferences;

    private NfcAdapter mNfcAdapter;
    private PendingIntent mNfcPendingIntent;
    private IntentFilter[] mNdefExchangeFilters;
    private boolean nfcConfigured = false;
    private boolean mWriteMode = false;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        pager = (ViewPager) findViewById(R.id.pager);
        adapter = new MyPagerAdapter(getSupportFragmentManager());

        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(2);

        tabs.setViewPager(pager);

        mSessionPreferences = getSharedPreferences(
                getString(R.string.session_shared_preferences_key), MODE_PRIVATE);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        configureNFC();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mNfcAdapter != null && mNfcAdapter.isEnabled()) {

            if (!nfcConfigured) configureNFC();

        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
                mNdefExchangeFilters, null);
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
                case 2:  return HQFragment.newInstance(position);
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
            case R.id.action_create:
                Intent intent = new Intent(this, UnlockGateActivity.class);
                intent.putParcelableArrayListExtra("networks", getNetworks());
                startActivity(intent);
                return true;
            case R.id.action_offer_advice:
                return true;
            case R.id.action_logout:
                logout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
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

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    final NetworksFragment networksFragment = (NetworksFragment) adapter.getRegisteredFragment(1);
                                    networksFragment.addNetworksToList(newNetworks);

                                    CharSequence[] items = networkNames.toArray(new CharSequence[networkNames.size()]);
                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                    builder.setTitle(gatekeeperName + " granted you access to these networks...")
                                           .setItems(items, new DialogInterface.OnClickListener() {
                                               @Override
                                               public void onClick(DialogInterface dialog, int which) {
                                                   dialog.dismiss();
                                                   networksFragment.adaptList();
                                                   showFeed(newNetworks.get(which));
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
                    Log.v("Somethin dun fucked up", "idk wut it wuz");
                }
            };

            APIRequestManager.getInstance().doRequest().grantAccessToNetworks(gatekeeperId, params, listener, errorListener);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    public void logout() {
        Toast.makeText(this, "Logging out", Toast.LENGTH_SHORT).show();
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

    public void showFeed(Network network) {
        pager.setCurrentItem(0, true);

        FeedFragment feedFragment = (FeedFragment) adapter.getRegisteredFragment(0);
        feedFragment.getNetworkFeed(network);
    }

    public ArrayList<Network> getNetworks() {
        NetworksFragment networksFragment = (NetworksFragment) adapter.getRegisteredFragment(1);
        return networksFragment.getNetworks();
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

}
