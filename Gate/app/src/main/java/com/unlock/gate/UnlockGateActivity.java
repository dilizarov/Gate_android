package com.unlock.gate;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.unlock.gate.models.Network;
import com.unlock.gate.utils.Fade;
import com.unlock.gate.utils.NfcUtils;

import java.util.ArrayList;

public class UnlockGateActivity extends Activity {

    private RelativeLayout networkSelector;
    private ListView networksList;
    private TextView ready;
    private TextView bumpPhones;
    private ArrayList<Network> networks;
    private ArrayList<String> selectedNetworkIds;
    private NfcAdapter mNfcAdapter;
    private SharedPreferences mSessionPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unlock_gate);

        networks = getIntent().getParcelableArrayListExtra("networks");
        mSessionPreferences = getSharedPreferences(
                getString(R.string.session_shared_preferences_key), MODE_PRIVATE);

        instantiateViews();

        bindNetworksToListView();

        setReadyClickListener();
    }

    private void instantiateViews() {
        networkSelector = (RelativeLayout) findViewById(R.id.networkSelector);
        networksList    = (ListView) findViewById(R.id.networksList);
        ready           = (TextView) findViewById(R.id.ready);
        bumpPhones      = (TextView) findViewById(R.id.bumpPhones);
    }

    private void bindNetworksToListView() {
        String[] networkNames = new String[networks.size()];

        int len = networks.size();
        for (int i = 0; i < len; i++)
            networkNames[i] = networks.get(i).getName();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_multiple_choice, networkNames);
        networksList.setAdapter(adapter);
        networksList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        adapter.notifyDataSetChanged();
    }

    private void setReadyClickListener() {
        ready.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        SparseBooleanArray checkedNetworks = networksList.getCheckedItemPositions();

                        if (checkedNetworks != null) {
                            int len = checkedNetworks.size();

                            Log.v("checkedNetworks", checkedNetworks.toString());

                            for (int i = 0; i < len; i++) {
                                if (checkedNetworks.valueAt(i)) {
                                    int key = checkedNetworks.keyAt(i);
                                    Log.v("network:", networks.get(key).getName());
                                    selectedNetworkIds.add(networks.get(key).getId());
                                }
                            }
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Fade.show(bumpPhones);
                                Fade.hide(networkSelector, new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        networkSelector.setAlpha(1);
                                        networkSelector.setVisibility(View.GONE);
                                        networkSelector.animate().setListener(null);
                                    }
                                });

                                mNfcAdapter = NfcAdapter.getDefaultAdapter(UnlockGateActivity.this);
                                if (mNfcAdapter != null & mNfcAdapter.isEnabled()) {
                                    String userId = mSessionPreferences.getString(getString(R.string.user_id_key), null);
                                    String userName = mSessionPreferences.getString(getString(R.string.user_name_key), null);
                                    String networkIds = selectedNetworkIds.toString();
                                    networkIds = networkIds.substring(1, networkIds.length() - 1);

                                    mNfcAdapter.setNdefPushMessage(
                                            NfcUtils.stringsToNdefMessage(userId, userName, networkIds), UnlockGateActivity.this);
                                }
                            }
                        });
                    }
                }).start();

            }
        });
    }

}
