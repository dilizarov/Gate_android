package com.unlock.gate;

import android.app.AlertDialog;
import android.app.PendingIntent;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.astuetz.PagerSlidingTabStrip;
import com.unlock.gate.models.Network;
import com.unlock.gate.models.Post;
import com.unlock.gate.utils.APIRequestManager;
import com.unlock.gate.utils.NfcUtils;
import com.unlock.gate.utils.VolleyErrorHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

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
    private boolean mWriteMode = false;

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

        mNfcPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);

        try {
            ndefDetected.addDataType("text/plain");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }

        mNdefExchangeFilters = new IntentFilter[] { ndefDetected };

        String userId     = mSessionPreferences.getString(getString(R.string.user_id_key), null);
        String userName   = mSessionPreferences.getString(getString(R.string.user_name_key), null);

        mNfcAdapter.setNdefPushMessage(
                NfcUtils.stringsToNdefMessage(userId, userName), MainActivity.this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mNfcAdapter != null && mNfcAdapter.isEnabled())
            mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
                mNdefExchangeFilters, null);
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
                switch (pager.getCurrentItem()) {
                    case 0: createPost(); break;
                    case 1: createNetwork(); break;
                    case 2: createKey(); break;
                }
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
            final String otherUserId   = NfcUtils.getNdefMessagePayload(messages[0]);
            final String otherUserName = NfcUtils.getNdefMessagePayload(messages[1]);

            Toast.makeText(MainActivity.this, otherUserId, Toast.LENGTH_LONG).show();
            //do what I would do.  messages[0] gets me what I want.

            final ArrayList<Integer> selectedNetworks = new ArrayList();
            final ArrayList<Network> userNetworks = getNetworks();

            int len = userNetworks.size();
            CharSequence[] networkNames = new CharSequence[len];

            for (int i = 0; i < len; i++) networkNames[i] = userNetworks.get(i).getName();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Grant Irene access to...")
                    .setMultiChoiceItems(networkNames, null,
                            new DialogInterface.OnMultiChoiceClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                    if (isChecked) {
                                        selectedNetworks.add(which);
                                    } else if (selectedNetworks.contains(which)) {
                                        selectedNetworks.remove(Integer.valueOf(which));
                                    }
                                }
                            })
                    .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ArrayList<String> grantedNetworkIds = new ArrayList<String>();
                            for (int network_index : selectedNetworks) {
                                grantedNetworkIds.add(userNetworks.get(network_index).getId());
                            }

                            grantAccessToNetworks(grantedNetworkIds, otherUserId);
                            dialog.dismiss();
                        }
                    });

            builder.create().show();
        }
    }

    public void grantAccessToNetworks(ArrayList<String> grantedNetworkIds, String otherUserId) {
        try {

            JSONObject params = new JSONObject();
            params.put("user_id", mSessionPreferences.getString(getString(R.string.user_id_key), null))
                  .put("auth_token", mSessionPreferences.getString(getString(R.string.user_auth_token_key), null))
                  .put("other_user_id", otherUserId)
                  .put("networks", new JSONArray(grantedNetworkIds));

            Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {

                }
            };

            Response.ErrorListener errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                        
                }
            };

            APIRequestManager.getInstance().doRequest().grantAccess(params, listener, errorListener);
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

}
