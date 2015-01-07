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
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.OvershootInterpolator;
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
    ImageView imgTap;


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
        imgTap   = (ImageView) findViewById(R.id.imgTap);
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

        final int originalPos1[] = new int[2];
        img1Side.getLocationOnScreen(originalPos1);

        final int originalPos2[] = new int[2];
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

        final int mmm = dm.widthPixels/2;
        final int xDest1 = dm.widthPixels/2 - img1Side.getMeasuredWidth();
        final int xDest2 = dm.widthPixels/2;

        final int yDest = dm.heightPixels/2 - (img1Side.getMeasuredHeight()/2) - statusBarOffset;

Log.v("width", Integer.toString(img1Side.getMeasuredWidth()));
        final TranslateAnimation anim1 = new TranslateAnimation(0, xDest1 - originalPos1[0], 0, /*yDest - originalPos1[1]*/0);
        final TranslateAnimation anim2 = new TranslateAnimation(0, xDest2 - originalPos2[0], 0, /*yDest - originalPos2[1]*/0);
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
                final int originalPos3[] = new int[2];
                imgTap.getLocationOnScreen(originalPos3);
                AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
                alphaAnimation.setDuration(500);

                TranslateAnimation anim3 = new TranslateAnimation(0, mmm - originalPos3[0] - imgTap.getMeasuredWidth() - img1Side.getMeasuredWidth() * .2f, 0, /*yDest - originalPos3[1] + img1Side.getHeight()/2*/0);
                anim3.setDuration(1000);
                anim3.setStartOffset(500);
                anim3.setFillAfter(true);
                anim3.setInterpolator(new OvershootInterpolator());

                AnimationSet animSet = new AnimationSet(false);
                animSet.addAnimation(alphaAnimation);
                animSet.addAnimation(anim3);
                animSet.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        AlphaAnimation ap = new AlphaAnimation(1, 0);
                        ap.setDuration(500);
                        ap.setFillAfter(true);

//                        ap.setAnimationListener(new Animation.AnimationListener() {
//                            @Override
//                            public void onAnimationStart(Animation animation) {
//
//                            }
//
//                            @Override
//                            public void onAnimationEnd(Animation animation) {
//                                img1.setVisibility(View.INVISIBLE);
//                                img1Side.setVisibility(View.INVISIBLE);
//                                img2.setVisibility(View.INVISIBLE);
//                                img2Side.setVisibility(View.INVISIBLE);
//                                imgTap.setVisibility(View.INVISIBLE);
//                                img1.clearAnimation();
//                                img1Side.clearAnimation();
//                                img2.clearAnimation();
//                                img2Side.clearAnimation();
//                                imgTap.clearAnimation();
//
//                                img1.setImageResource(R.drawable.ic_hardware_phone_android);
//                                img2.setImageResource(R.drawable.ic_hardware_phone_android_unlocked);
//
//                                AlphaAnimation alphaAnimation1 = new AlphaAnimation(0, 1);
//                                alphaAnimation1.setDuration(500);
//                                alphaAnimation1.setFillAfter(true);
//
//                                img1.setAnimation(alphaAnimation1);
//                                img2.setAnimation(alphaAnimation1);
//                            }
//
//                            @Override
//                            public void onAnimationRepeat(Animation animation) {
//
//                            }
//                        });

                        imgTap.setAnimation(ap);
//                        img1Side.setAnimation(ap);
//                        img2Side.setAnimation(ap);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                imgTap.setAnimation(animSet);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        final float centerX = img1.getWidth() / 2.0f;
        final float centerY = img1.getHeight() / 2.0f;

        final Rotate3dAnimation rotationLeft = new Rotate3dAnimation(0, -90, centerX, centerY, 0, true);
        final Rotate3dAnimation rotationRight = new Rotate3dAnimation(0, 90, centerX, centerY, 0, true);
        rotationLeft.setDuration(1000);
        rotationRight.setDuration(1000);
        rotationLeft.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                img1.setVisibility(View.INVISIBLE);
                img1Side.setVisibility(View.VISIBLE);
                TranslateAnimation trAnm = new TranslateAnimation(0, -(xDest1 - originalPos1[0]), 0, 0);
                trAnm.setDuration(1000);
                trAnm.setStartOffset(3500);

                trAnm.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        Rotate3dAnimation rotateBackLeft = new Rotate3dAnimation(-90, 0, centerX, centerY, 0, true);

                        img1Side.setVisibility(View.INVISIBLE);
                        rotateBackLeft.setDuration(1000);
                        rotateBackLeft.setFillAfter(true);
                        img1.setAnimation(rotateBackLeft);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

                AnimationSet set = new AnimationSet(false);
                set.addAnimation(anim1);
                set.addAnimation(trAnm);

                img1Side.startAnimation(set);
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
                TranslateAnimation trAnm = new TranslateAnimation(0, -(xDest2 - originalPos2[0]), 0, 0);
                trAnm.setDuration(1000);
                trAnm.setStartOffset(3500);

                trAnm.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        Rotate3dAnimation rotateBackRight = new Rotate3dAnimation(90, 0, centerX, centerY, 0, true);

                        img2Side.setVisibility(View.INVISIBLE);
                        rotateBackRight.setDuration(1000);
                        rotateBackRight.setFillAfter(true);
                        img2.setImageResource(R.drawable.ic_hardware_phone_android_unlocked);
                        img2.setAnimation(rotateBackRight);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

                AnimationSet set = new AnimationSet(false);
                set.addAnimation(anim2);
                set.addAnimation(trAnm);

                img2Side.startAnimation(set);
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
