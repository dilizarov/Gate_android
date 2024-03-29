package com.unlock.gate;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.Menu;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.unlock.gate.models.Gate;
import com.unlock.gate.models.Key;
import com.unlock.gate.utils.APIRequestManager;
import com.unlock.gate.utils.Butter;
import com.unlock.gate.utils.Fade;
import com.unlock.gate.utils.NfcUtils;
import com.unlock.gate.utils.Rotate3dAnimation;
import com.unlock.gate.utils.VolleyErrorHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UnlockGateActivity extends ActionBarActivity {

    private RelativeLayout gateSelector;
    private ListView gatesList;
    private Button unlock;
    private ArrayList<Gate> gates;
    private ArrayList<String> selectedGateIds;
    private NfcAdapter mNfcAdapter;
    private SharedPreferences mSessionPreferences;
    private android.support.v7.app.ActionBar actionBar;
    private TextView noGatesText;

    private LinearLayout progressBarHolder;

    private MenuItem refreshButton;

    private ImageView leftPhone;
    private ImageView leftPhoneSide;
    private ImageView rightPhone;
    private ImageView rightPhoneSide;
    private ImageView tapPhone;
    private TextView tutorialText;

    private int screenCenterVert;
    private int leftSideDestination;
    private int[] leftOriginalPosition;
    private int[] rightOriginalPosition;
    private int[] tapPhonePosition;
    private float phoneCenterX;
    private float phoneCenterY;
    private boolean metricsCalculated;

    private boolean animating;

    private final int STEP_DURATION = 1500;
    private final int FADE_DURATION = 500;

    private ArrayList<Key> keysToAdd;

    // Like in MainActivity, if using status bar to change NFC status, we appropriate what happens here.
    // Otherwise, fall back to usual onResume()/onPause() lifecycle
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)) {
                setButtonTextBasedOnNFCState();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unlock_gate);

        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#000000")));

        gates = getIntent().getParcelableArrayListExtra("gates");
        mSessionPreferences = getSharedPreferences(
                getString(R.string.session_shared_preferences_key), MODE_PRIVATE);

        selectedGateIds = new ArrayList<String>();

        keysToAdd = new ArrayList<Key>();

        instantiateViews();

        setUnlockClickListener();

        mNfcAdapter = NfcAdapter.getDefaultAdapter(UnlockGateActivity.this);

        // Phone doesn't support NFC
        if (mNfcAdapter == null || !mNfcAdapter.isEnabled()) {
            unlock.setText("Tap for a key");
        }

        if (gates.size() > 0) {
            progressBarHolder.setVisibility(View.GONE);
            adaptNewGatesToList();
        } else {
            requestGatesAndPopulateListView(false);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        setButtonTextBasedOnNFCState();

        if (mNfcAdapter != null) {
            IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
            this.registerReceiver(mReceiver, filter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mNfcAdapter != null) {
            this.unregisterReceiver(mReceiver);
        }
    }

    private void setButtonTextBasedOnNFCState() {
        if (mNfcAdapter != null && !animating) {
            if (mNfcAdapter.isEnabled()) {
                unlock.setText("Unlock");
            } else {
                unlock.setText("Tap for a key");
            }
        }
    }

    private void instantiateViews() {
        gateSelector = (RelativeLayout) findViewById(R.id.gateSelector);
        gatesList    = (ListView) findViewById(R.id.gatesList);
        unlock       = (Button) findViewById(R.id.unlockButton);
        noGatesText  = (TextView) findViewById(R.id.noUnlockGatesMessage);

        progressBarHolder = (LinearLayout) findViewById(R.id.unlockGatesProgressBarHolder);

        leftPhone      = (ImageView) findViewById(R.id.leftPhoneImage);
        rightPhone     = (ImageView) findViewById(R.id.rightPhoneImage);
        leftPhoneSide  = (ImageView) findViewById(R.id.leftPhoneSideImage);
        rightPhoneSide = (ImageView) findViewById(R.id.rightPhoneSideImage);
        tapPhone       = (ImageView) findViewById(R.id.tapPhoneImage);
        tutorialText   = (TextView) findViewById(R.id.tutorialText);
    }

    private void adaptNewGatesToList() {
        noGatesText.setText(R.string.no_gates_default);
        noGatesText.setVisibility(
                gates.size() == 0
                        ? View.VISIBLE
                        : View.INVISIBLE
        );

        String[] gateNames = new String[gates.size()];

        int len = gates.size();
        for (int i = 0; i < len; i++)
            gateNames[i] = gates.get(i).getName();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.simple_list_item_ellipsized_multiple_choice, gateNames);

        gatesList.setAdapter(adapter);
        gatesList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        adapter.notifyDataSetChanged();
    }

    private void setUnlockClickListener() {

        unlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int len = gatesList.getCount();
                for (int i = 0; i < len; i++) {

                    if (gatesList.isItemChecked(i)) {
                        selectedGateIds.add(gates.get(i).getId());
                    }
                }

                if (selectedGateIds.size() == 0) {
                    Butter.between(UnlockGateActivity.this,
                            "You must unlock at least one gate");

                    return;
                }

                // During the animation or if NFC not supported, opt for key
                if (animating || mNfcAdapter == null || !mNfcAdapter.isEnabled())
                    generateShowKey();
                else {
                    actionBar.setTitle("Tutorial");
                    Fade.hide(gateSelector, new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            animateTutorial();
                        }
                    });

                    if (mNfcAdapter != null && mNfcAdapter.isEnabled()) {

                        String userId = mSessionPreferences.getString(getString(R.string.user_id_key), null);
                        String userName = mSessionPreferences.getString(getString(R.string.user_name_key), null);
                        String gateIds = selectedGateIds.toString();
                        gateIds = gateIds.substring(1, gateIds.length() - 1);

                        mNfcAdapter.setNdefPushMessage(
                            NfcUtils.stringsToNdefMessage(userId, userName, gateIds), UnlockGateActivity.this);
                    }

                }
            }
        });
    }

    private void requestGatesAndPopulateListView(final boolean refreshing) {

        JSONObject params = new JSONObject();

        Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(final JSONObject response) {

                new Thread(new Runnable() {

                    public void run() {
                        gates.clear();
                        selectedGateIds.clear();

                        JSONArray jsonGates = response.optJSONArray("gates");
                        int len = jsonGates.length();
                        for (int i = 0; i < len; i++) {
                            JSONObject jsonGate = jsonGates.optJSONObject(i);

                            String creator;

                            if (jsonGate.optJSONObject("creator") != null) {
                                creator = jsonGate.optJSONObject("creator").optString("name");
                            } else {
                                creator = "";
                            }

                            Gate gate = new Gate(jsonGate.optString("external_id"),
                                    jsonGate.optString("name"),
                                    jsonGate.optInt("users_count"),
                                    creator,
                                    jsonGate.optBoolean("generated"),
                                    jsonGate.optBoolean("session"),
                                    jsonGate.optBoolean("unlocked_perm"));

                            gates.add(gate);
                        }

                        runOnUiThread(new Runnable() {
                            public void run() {
                                adaptNewGatesToList();
                                progressBarHolder.setVisibility(View.GONE);

                                if (refreshing && refreshButton != null) {
                                    gatesList.setSelection(0);
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

                if (gates.size() == 0) {
                    if (volleyError.isConnectionError())
                        noGatesText.setText(R.string.volley_no_connection_error);
                    else
                        noGatesText.setText(R.string.gate_error_message);

                    noGatesText.setVisibility(View.VISIBLE);
                }

                Butter.down(UnlockGateActivity.this, volleyError.getMessage());

                if (refreshing && refreshButton != null) {
                    refreshButton.setActionView(null);
                }
            }
        };

        APIRequestManager.getInstance().doRequest().getGates(params, listener, errorListener);

    }

    private void calculateMetrics() {
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

    private void kickOffAnimation() {

        unlock.setText("Tap for a key instead");
        animating = true;
        // 0 denotes the left phone, 1 denotes the right phone.
        if (leftPhone.getVisibility() == View.VISIBLE && rightPhone.getVisibility() == View.VISIBLE) {
            rotatePhone(0);
            rotatePhone(1);
        } else {
            Fade.setDuration(FADE_DURATION);
            Fade.show(tutorialText);
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
                translateSideBack.setStartOffset(2 * (STEP_DURATION + FADE_DURATION));

                translateSideBack.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        Rotate3dAnimation rotateBack = new Rotate3dAnimation(phone == 0 ? -90 : 90, 0, phoneCenterX, phoneCenterY, 0, true);

                        Fade.hide(phoneSide(phone));

                        rotateBack.setDuration(STEP_DURATION);
                        rotateBack.setFillAfter(true);
                        if (phone == 1)
                            rightPhone.setImageResource(R.drawable.ic_hardware_phone_android_unlocked);

                        phone(phone).setVisibility(View.VISIBLE);
                        phone(phone).setAnimation(rotateBack);

                        tutorialText.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                setReplayTutorialClickListener();
                            }
                        }, STEP_DURATION * 3);
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
    }

    private void setReplayTutorialClickListener() {
        tutorialText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlphaAnimation fadeOut = new AlphaAnimation(1, 0);
                fadeOut.setDuration(FADE_DURATION);
                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        AlphaAnimation fadeOutText = new AlphaAnimation(1, 0);
                        fadeOutText.setDuration(FADE_DURATION);
                        fadeOutText.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                tutorialText.setVisibility(View.INVISIBLE);
                                tutorialText.setText(R.string.tutorial_text);
                                tutorialText.setOnClickListener(null);
                                AlphaAnimation fadeInText = new AlphaAnimation(0, 1);
                                fadeInText.setDuration(FADE_DURATION);
                                fadeInText.setAnimationListener(new Animation.AnimationListener() {
                                    @Override
                                    public void onAnimationStart(Animation animation) {
                                    }

                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                        tutorialText.setVisibility(View.VISIBLE);
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animation animation) {
                                    }
                                });

                                tutorialText.setAnimation(fadeInText);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }
                        });

                        tutorialText.setAnimation(fadeOutText);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        rightPhone.setVisibility(View.INVISIBLE);
                        rightPhone.setImageResource(R.drawable.ic_hardware_phone_android);
                        AlphaAnimation fadeIn = new AlphaAnimation(0, 1);
                        fadeIn.setDuration(FADE_DURATION);
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

        tutorialText.setText("Press to replay tutorial");
    }

    private ImageView phone(int phone) {
        return phone == 0 ? leftPhone : rightPhone;
    }

    private ImageView phoneSide(int phone) {
        return phone == 0 ? leftPhoneSide : rightPhoneSide;
    }

    private void generateShowKey() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.progress_dialog_server_processing_request));
        progressDialog.setIndeterminate(true);
        progressDialog.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progress));
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);

        progressDialog.show();

        try {
            JSONObject params = new JSONObject();
            JSONObject key = new JSONObject();

            key.put("gates", new JSONArray(selectedGateIds));

            params.put("key", key);

            Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    progressDialog.dismiss();
                    final String key = response.optJSONObject("key").optString("key");
                    JSONArray gates = response.optJSONObject("key").optJSONArray("gates");

                    addToKeysToAdd(response.optJSONObject("key"));

                    final StringBuilder gatesBuilder = new StringBuilder();
                    int len = gates.length();
                    if (len == 1) {
                        gatesBuilder.append(gates.optJSONObject(0).optString("name"));
                    } else if (len == 2) {
                        gatesBuilder.append(gates.optJSONObject(0).optString("name"))
                                    .append(" and ")
                                    .append(gates.optJSONObject(1).optString("name"));
                    } else if (len > 2) {
                        for (int i = 0; i < len; i++) {
                            if (i != 0) gatesBuilder.append(", ");
                            if (i == len - 1) gatesBuilder.append("and ");
                            gatesBuilder.append(gates.optJSONObject(i).optString("name"));
                        }
                    }

                    MaterialDialog dialog = new MaterialDialog.Builder(UnlockGateActivity.this)
                            .title("Key")
                            .content(Html.fromHtml("<b>" + key + "</b> unlocks " + gatesBuilder.toString() + "<br><br> Keys expire 3 days after not being used"))
                            .cancelable(false)
                            .autoDismiss(false)
                            .positiveText("SHARE")
                            .neutralText("COPY")
                            .negativeText("CANCEL")
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    String extraText = "Use " + key + " to #unlock " + gatesBuilder.toString() + " on #Gate\n\nhttp://unlockgate.today";
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
                                        } else if (packageName.contains("twitter")    ||
                                                packageName.contains("mms")        ||
                                                packageName.contains("sms")        ||
                                                packageName.contains("android.gm") ||
                                                packageName.contains("skype")      ||
                                                packageName.contains("tumblr")) {

                                            Intent intent = new Intent();
                                            intent.setComponent(new ComponentName(packageName, ri.activityInfo.name));
                                            intent.setAction(Intent.ACTION_SEND);
                                            intent.setType("text/plain");
                                            intent.putExtra(Intent.EXTRA_SUBJECT, extraSubject);

                                            if (packageName.contains("mms") || packageName.contains("sms"))
                                                intent.putExtra(Intent.EXTRA_TEXT, key);
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

                                @Override
                                public void onNeutral(MaterialDialog dialog) {
                                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                    ClipData clip = ClipData.newPlainText("key", key);
                                    clipboard.setPrimaryClip(clip);
                                    Butter.down(UnlockGateActivity.this, key + " saved to clipboard");
                                }
                            }).show();

                }
            };

            Response.ErrorListener errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    progressDialog.dismiss();
                    VolleyErrorHandler volleyError = new VolleyErrorHandler(error);

                    Butter.down(UnlockGateActivity.this, volleyError.getMessage());
                }
            };

            APIRequestManager.getInstance().doRequest().getKey(params, listener, errorListener);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    private void addToKeysToAdd(JSONObject jsonKey) {
        ArrayList<String> gateNames = new ArrayList<String>();
        JSONArray jsonGates = jsonKey.optJSONArray("gates");
        for (int j = 0; j < jsonGates.length(); j++) {
            gateNames.add(jsonGates.optJSONObject(j).optString("name"));
        }

        Key key = new Key(jsonKey.optString("key"),
                jsonKey.optString("updated_at"),
                gateNames);

        keysToAdd.add(key);
    }

    @Override
    public void onBackPressed() {
        Intent intent = getIntent();
        if (keysToAdd.size() > 0) {
            intent.putParcelableArrayListExtra("keys", keysToAdd);
            setResult(RESULT_OK, intent);
        } else {
            setResult(RESULT_CANCELED, intent);
        }

        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_unlock_gates, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_refresh_gates:
                if (refreshButton == null) refreshButton = item;
                ProgressBar progressBar = new ProgressBar(this);
                progressBar.setIndeterminate(true);
                progressBar.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progress_action_bar));
                refreshButton.setActionView(progressBar);
                requestGatesAndPopulateListView(true);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
