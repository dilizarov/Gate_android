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
import android.widget.TextView;

import com.unlock.gate.models.Network;
import com.unlock.gate.utils.Butter;
import com.unlock.gate.utils.Fade;
import com.unlock.gate.utils.NfcUtils;
import com.unlock.gate.utils.Rotate3dAnimation;

import java.util.ArrayList;

public class UnlockGateActivity extends Activity {

    private RelativeLayout networkSelector;
    private ListView networksList;
    private Button unlock;
    private ArrayList<Network> networks;
    private ArrayList<String> selectedNetworkIds;
    private NfcAdapter mNfcAdapter;
    private SharedPreferences mSessionPreferences;
    private ActionBar actionBar;

    ImageView leftPhone;
    ImageView leftPhoneSide;
    ImageView rightPhone;
    ImageView rightPhoneSide;
    ImageView tapPhone;

    private int screenCenterVert;
    private int leftSideDestination;
    private int[] leftOriginalPosition;
    private int[] rightOriginalPosition;
    private int[] tapPhonePosition;
    private float phoneCenterX;
    private float phoneCenterY;
    private boolean metricsCalculated;

    private final int STEP_DURATION = 2000;
    private final int FADE_DURATION = 500;

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
        unlock          = (Button) findViewById(R.id.unlockButton);

        leftPhone      = (ImageView) findViewById(R.id.leftPhoneImage);
        rightPhone     = (ImageView) findViewById(R.id.rightPhoneImage);
        leftPhoneSide  = (ImageView) findViewById(R.id.leftPhoneSideImage);
        rightPhoneSide = (ImageView) findViewById(R.id.rightPhoneSideImage);
        tapPhone       = (ImageView) findViewById(R.id.tapPhoneImage);
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

        TextView lala = (TextView) findViewById(R.id.lalalala);

        lala.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlphaAnimation fadeOut = new AlphaAnimation(1, 0);
                fadeOut.setDuration(500);
                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        rightPhone.setVisibility(View.INVISIBLE);
                        rightPhone.setImageResource(R.drawable.ic_hardware_phone_android);
                        AlphaAnimation fadeIn = new AlphaAnimation(0, 1);
                        fadeIn.setDuration(500);
                        fadeIn.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                rightPhone.setVisibility(View.VISIBLE);
                                animateTutorial();
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }
                        });

                        rightPhone.startAnimation(fadeIn);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });

                rightPhone.startAnimation(fadeOut);
            }
        });

        unlock.setOnClickListener(new View.OnClickListener() {
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

                                        animateTutorial();

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

    public void calculateMetrics() {
        DisplayMetrics dm = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(dm);

        screenCenterVert =  dm.widthPixels/2;
        leftSideDestination = screenCenterVert - leftPhoneSide.getMeasuredWidth();
        //rightSideDestination is screenCenterVert

        leftOriginalPosition = new int[2];
        leftPhoneSide.getLocationOnScreen(leftOriginalPosition);

        rightOriginalPosition = new int[2];
        rightPhoneSide.getLocationOnScreen(rightOriginalPosition);

        tapPhonePosition = new int[2];
        tapPhone.getLocationOnScreen(tapPhonePosition);

        phoneCenterX = leftPhone.getWidth() / 2.0f;
        phoneCenterY = leftPhone.getHeight() / 2.0f;

        metricsCalculated = true;
    }

    public void animateTutorial() {

        if (!metricsCalculated) calculateMetrics();
        kickOffAnimation();

    }

    public void kickOffAnimation() {
        // 0 denotes the left phone, 1 denotes the right phone.
        if (leftPhone.getVisibility() == View.VISIBLE && rightPhone.getVisibility() == View.VISIBLE) {
            rotatePhone(0);
            rotatePhone(1);
        } else {
            leftPhone.startAnimation(fadePhoneAnimation(0));
            rightPhone.startAnimation(fadePhoneAnimation(1));
        }
    }

    private AlphaAnimation fadePhoneAnimation(final int phone) {
        AlphaAnimation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(FADE_DURATION);
        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                phone(phone).setVisibility(View.VISIBLE);
                rotatePhone(phone);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        return fadeIn;
    }

    private void rotatePhone(final int phone) {
        final Rotate3dAnimation rotation = new Rotate3dAnimation(0, phone == 0 ? -90 : 90, phoneCenterX, phoneCenterY, 0, true);
        rotation.setDuration(STEP_DURATION);
        rotation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                phone(phone).setVisibility(View.INVISIBLE);
                phoneSide(phone).setVisibility(View.VISIBLE);

                // Moves the side view back when the intermediate animation (tapping) is complete.

                int xDelta = (phone == 0)
                ? -(leftSideDestination - leftOriginalPosition[0])
                : -(screenCenterVert - rightOriginalPosition[0]);

                TranslateAnimation translateSideBack = new TranslateAnimation(0, xDelta, 0, 0);
                translateSideBack.setDuration(STEP_DURATION);
                translateSideBack.setStartOffset(5000);

                translateSideBack.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        Rotate3dAnimation rotateBack = new Rotate3dAnimation(phone == 0 ? -90 : 90, 0, phoneCenterX, phoneCenterY, 0, true);

                        phoneSide(phone).setVisibility(View.INVISIBLE);
                        rotateBack.setDuration(STEP_DURATION);
                        rotateBack.setFillAfter(true);
                        if (phone == 1)
                            rightPhone.setImageResource(R.drawable.ic_hardware_phone_android_unlocked);

                        phone(phone).setVisibility(View.VISIBLE);
                        phone(phone).setAnimation(rotateBack);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });

                AnimationSet set = new AnimationSet(false);
                set.addAnimation(translateSide(phone));
                set.addAnimation(translateSideBack);

                phoneSide(phone).startAnimation(set);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        phone(phone).startAnimation(rotation);
    }

    private TranslateAnimation translateSide(int phone) {
        int xDelta = (phone == 0)
        ? leftSideDestination - leftOriginalPosition[0]
        : screenCenterVert - rightOriginalPosition[0];

        TranslateAnimation translateSide = new TranslateAnimation(0, xDelta, 0, 0);
        translateSide.setDuration(STEP_DURATION);
        translateSide.setFillAfter(true);

        if (phone == 0) translateSide.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                actionBar.setTitle("Tap your phone...");

                AlphaAnimation fadeIn = new AlphaAnimation(0, 1);
                fadeIn.setDuration(FADE_DURATION);

                TranslateAnimation translateTap = new TranslateAnimation(0, screenCenterVert - tapPhonePosition[0] - tapPhone.getMeasuredWidth() - leftPhoneSide.getMeasuredWidth() * .2f, 0, 0);
                translateTap.setDuration(STEP_DURATION);
                translateTap.setStartOffset(FADE_DURATION);
                translateTap.setFillAfter(true);
                translateTap.setInterpolator(new OvershootInterpolator());

                AnimationSet animSet = new AnimationSet(false);
                animSet.addAnimation(fadeIn);
                animSet.addAnimation(translateTap);
                animSet.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        AlphaAnimation fadeOut = new AlphaAnimation(1, 0);
                        fadeOut.setDuration(FADE_DURATION);
                        fadeOut.setFillAfter(true);

                        tapPhone.setAnimation(fadeOut);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });

                tapPhone.setAnimation(animSet);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        return translateSide;
    };

    private ImageView phone(int phone) {
        return phone == 0 ? leftPhone : rightPhone;
    }

    private ImageView phoneSide(int phone) {
        return phone == 0 ? leftPhoneSide : rightPhoneSide;
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
