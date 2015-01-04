package com.unlock.gate;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.unlock.gate.models.Network;
import com.unlock.gate.utils.Butter;
import com.unlock.gate.utils.Fade;
import com.unlock.gate.utils.NfcUtils;
import com.unlock.gate.utils.Rotate3dAnimation;

import java.util.ArrayList;

public class UnlockGateActivity extends Activity {

    private RelativeLayout networkSelector;
    private ListView networksList;
    private Button ready;
    private ArrayList<Network> networks;
    private ArrayList<String> selectedNetworkIds;
    private NfcAdapter mNfcAdapter;
    private SharedPreferences mSessionPreferences;
    private ActionBar actionBar;

    ImageView img1;
    ImageView img1Side;
    ImageView img2;
    ImageView img2Side;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unlock_gate);

        actionBar = getActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#000000")));

        networks = getIntent().getParcelableArrayListExtra("networks");
        mSessionPreferences = getSharedPreferences(
                getString(R.string.session_shared_preferences_key), MODE_PRIVATE);

        selectedNetworkIds = new ArrayList<String>();

        instantiateViews();

        bindNetworksToListView();

        setReadyClickListener();
    }

    private void instantiateViews() {
        networkSelector = (RelativeLayout) findViewById(R.id.networkSelector);
        networksList    = (ListView) findViewById(R.id.networksList);
        ready           = (Button) findViewById(R.id.ready);

        img1     = (ImageView) findViewById(R.id.img1);
        img2     = (ImageView) findViewById(R.id.img2);
        img1Side = (ImageView) findViewById(R.id.img1side);
        img2Side = (ImageView) findViewById(R.id.img2side);
    }

    private void bindNetworksToListView() {
        String[] networkNames = new String[networks.size()];

        int len = networks.size();
        for (int i = 0; i < len; i++)
            networkNames[i] = networks.get(i).getName();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.simple_list_item_ellipsized_multiple_choice, networkNames);
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

                        int len = networksList.getCount();
                        for (int i = 0; i < len; i++) {

                              if (networksList.isItemChecked(i)) {
                                  Log.v("checked item", networks.get(i).getName());
                                  selectedNetworkIds.add(networks.get(i).getId());
                              }
                        }


                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                if (selectedNetworkIds.size() == 0) {
                                    Butter.between(UnlockGateActivity.this,
                                            "You must unlock at least one gate");

                                    return;
                                }

                                actionBar.setTitle("Bump phones...");

                                Fade.hide(networkSelector, new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        networkSelector.setAlpha(1);
                                        networkSelector.setVisibility(View.GONE);
                                        networkSelector.animate().setListener(null);

                                        animatePhoneBumping();

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

    public void animatePhoneBumping() {
        img1.setVisibility(View.VISIBLE);
        img2.setVisibility(View.VISIBLE);

        RelativeLayout root = (RelativeLayout) findViewById(R.id.rootLayout);
        DisplayMetrics dm = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(dm);

        Log.v("Height", Integer.toString(dm.heightPixels));
        Log.v("Width", Integer.toString(dm.widthPixels));
        Log.v("Measured Height", Integer.toString(root.getMeasuredHeight()));
        int statusBarOffset = dm.heightPixels - root.getMeasuredHeight() - actionBar.getHeight();

        int originalPos1[] = new int[2];
        img1Side.getLocationOnScreen(originalPos1);

        int originalPos2[] = new int[2];
        img2Side.getLocationOnScreen(originalPos2);

        int height = img1.getMeasuredHeight();
        int width  = img1.getMeasuredHeight()/8;

//        ViewGroup.LayoutParams layoutParams = img1Side.getLayoutParams();
//        layoutParams.height = height;
//        layoutParams.width = width;
//        img1Side.setLayoutParams(layoutParams);
//
//        layoutParams = img2Side.getLayoutParams();
//        layoutParams.height = height;
//        layoutParams.width = width;
//        img2Side.setLayoutParams(layoutParams);

        int xDest1 = dm.widthPixels/2 - img1Side.getMeasuredWidth();
        int xDest2 = dm.widthPixels/2;

        int yDest = dm.heightPixels/2 - (img1Side.getMeasuredHeight()/2) - statusBarOffset;


        final TranslateAnimation anim1 = new TranslateAnimation(0, xDest1 - originalPos1[0], 0, yDest - originalPos1[1]);
        final TranslateAnimation anim2 = new TranslateAnimation(0, xDest2 - originalPos2[0], 0, yDest - originalPos2[1]);
        anim1.setDuration(1000);
        anim2.setDuration(1000);
        anim1.setFillAfter(true);
        anim2.setFillAfter(true);

        anim1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                actionBar.setTitle("Tap your phone...");
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        float centerX = img1.getWidth() / 2.0f;
        float centerY = img1.getHeight() / 2.0f;

        final Rotate3dAnimation rotationLeft = new Rotate3dAnimation(0, -90, centerX, centerY, 0, true);
        final Rotate3dAnimation rotationRight = new Rotate3dAnimation(0, 90, centerX, centerY, 0, true);
        rotationLeft.setDuration(10000);
        rotationRight.setDuration(10000);
        rotationLeft.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                img1.setVisibility(View.INVISIBLE);
                img1Side.setVisibility(View.VISIBLE);
                img1Side.startAnimation(anim1);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        rotationRight.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                img2.setVisibility(View.INVISIBLE);
                img2Side.setVisibility(View.VISIBLE);
                img2Side.startAnimation(anim2);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        img1.startAnimation(rotationLeft);
        img2.startAnimation(rotationRight);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
