package com.unlock.gate;

import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.unlock.gate.utils.APIRequestManager;
import com.unlock.gate.utils.Butter;
import com.unlock.gate.utils.CustomEditText;
import com.unlock.gate.utils.CustomValidator;
import com.unlock.gate.utils.Fade;
import com.unlock.gate.utils.RegexConstants;
import com.unlock.gate.utils.SetErrorBugFixer;
import com.unlock.gate.utils.VolleyErrorHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class LoginRegisterActivity extends ActionBarActivity {
	
	private enum State {
		LOGIN, REGISTRATION, FORGOT_PASSWORD
	}

	private final int EMAIL_REQUEST_INTENT = 1;

	private SharedPreferences mActivityPreferences;
	private SharedPreferences mSessionPreferences;
	
	private String mEmail;
	private String mPassword;
	private String mFullName;
	
	private CustomEditText userEmail;
	private CustomEditText userPassword;
	private CustomEditText userFullName;
	
	private TextView forgotPassword;
	private TextView terms;
	private TextView toggleRegistrationLogin;
	
	private Button commandButton;
	
	private State viewState;

    private ProgressDialog progressDialog;

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private GoogleCloudMessaging gcm;
    private String regId;
    private final static String PROPERTY_REG_ID = "registration_id";
    private final static String PROPERTY_APP_VERSION = "app_version";
    private final String SENDER_ID = "222761912510";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login_register);

		mActivityPreferences = getSharedPreferences(
				getString(R.string.login_register_shared_preferences_key), MODE_PRIVATE);
		
		mSessionPreferences  = getSharedPreferences(
				getString(R.string.session_shared_preferences_key), MODE_PRIVATE); 

        if (mSessionPreferences.contains(getString(R.string.user_auth_token_key))) {
            Intent intent = new Intent(LoginRegisterActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            instantiateViews();

            if (savedInstanceState != null) {
                viewState = State.valueOf(savedInstanceState.getString("viewState"));
                if (viewState == State.REGISTRATION) {
                    forgotPassword.setVisibility(View.INVISIBLE);
                    userFullName.setVisibility(View.VISIBLE);
                    commandButton.setText(R.string.register);
                    toggleRegistrationLogin.setText(R.string.toggle_login);
                } else if (viewState == State.FORGOT_PASSWORD) {
                    forgotPassword.setVisibility(View.INVISIBLE);
                    userPassword.setVisibility(View.INVISIBLE);
                    commandButton.setText(R.string.send_email);
                    toggleRegistrationLogin.setText(R.string.toggle_login);

                }
            } else {
                viewState = State.LOGIN;
            }

            // External services just make it easier to get and set at the same time.
            getAndSetEmail();

            // handling sets up event listeners and actions.
            // Only handleCommandButton actually communicates to the server
            handleForgotPassword();
            handleTerms();
            handleToggleLoginRegistration();
            handleCommandButton();

            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(getString(R.string.progress_dialog_server_processing_request));
            progressDialog.setIndeterminate(true);
            progressDialog.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progress));
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
        }

	}

    @Override
    protected void onResume() {
        super.onResume();

        checkPlayServices();
    }
	
	private void instantiateViews() {
		userEmail    = (CustomEditText) findViewById(R.id.userEmail);
		userPassword = (CustomEditText) findViewById(R.id.userPassword);
		userFullName = (CustomEditText) findViewById(R.id.userFullName);
		
		//Required due to Android bug:
		//Essentially, depending on the keyboard user uses (like SwiftKey, etc.)
		//There is no guarantee that error goes away on text changed.
		userEmail.addTextChangedListener(new SetErrorBugFixer(userEmail));
		userPassword.addTextChangedListener(new SetErrorBugFixer(userPassword));
		userFullName.addTextChangedListener(new SetErrorBugFixer(userFullName));
		
		forgotPassword          = (TextView) findViewById(R.id.forgotPassword);
		terms                   = (TextView) findViewById(R.id.terms);
		toggleRegistrationLogin = (TextView) findViewById(R.id.toggleRegistrationLogin);
		
		commandButton = (Button) findViewById(R.id.commandButton);
	}
	
	private void handleForgotPassword() {
		//Fade out password bar.
		//Change command to send
		//Change bottom left to toggle to login

		forgotPassword.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				forgotPassword.setVisibility(View.INVISIBLE);
				Fade.hide(userPassword, new AnimatorListenerAdapter() {

					public void onAnimationStart(Animator animation) {
						commandButton.setText(R.string.send_email);
						toggleRegistrationLogin.setText(R.string.toggle_login);
						viewState = State.FORGOT_PASSWORD;

					}
				});
			}
		});
	}
	
	private void handleTerms() {
		terms.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Butter.down(LoginRegisterActivity.this, "Robots are hard at work getting these documents in order. They should be finished by the Beta version of Gate!");
			}
		});
	}
	
	private void handleToggleLoginRegistration() {
		toggleRegistrationLogin.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//IF ON LOGIN
				////hide 'forgot password'
				////Fade in Full Name
				////Change command to register
				////Change bottom left to toggle login
				//ELSE IF ON REGISTER
				////fade out Full Name
				////show forgot password
				////change command to login
				////change button left to toggle register
				
				if (viewState == State.LOGIN) {
					forgotPassword.setVisibility(View.INVISIBLE);
					Fade.show(userFullName, new AnimatorListenerAdapter() {
						public void onAnimationStart(Animator animation) {
							commandButton.setText(R.string.register);
							toggleRegistrationLogin.setText(R.string.toggle_login);
							viewState = State.REGISTRATION;
						}
					});
					
				} else {
					if (viewState == State.FORGOT_PASSWORD) {
						Fade.show(userPassword, new AnimatorListenerAdapter() {
							public void onAnimationStart(Animator animation) {
								forgotPassword.setVisibility(View.VISIBLE);
								commandButton.setText(R.string.log_in);
								toggleRegistrationLogin.setText(R.string.toggle_registration);
								viewState = State.LOGIN;
							}
						});

					} else {
						Fade.hide(userFullName, new AnimatorListenerAdapter() {
							public void onAnimationEnd(Animator animation) {
								forgotPassword.setVisibility(View.VISIBLE);
							}
							
							public void onAnimationStart(Animator animation) {
								commandButton.setText(R.string.log_in);
								toggleRegistrationLogin.setText(R.string.toggle_registration);
								viewState = State.LOGIN;
							}
						});
					}
				}
			}
		});
	}
	
	private void handleCommandButton() {

		commandButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				switch (viewState) {
					case LOGIN:
						processLogin();
						break;

					case REGISTRATION:
						processRegistration();
						break;

					case FORGOT_PASSWORD:
						processForgotPassword();
						break;
				}
			}
		});
	}
	
	private void processLogin() {
		mEmail    = userEmail.getText().toString();
		mPassword = userPassword.getText().toString();
		
		if (mEmail.length() == 0 || mPassword.length() == 0 || !CustomValidator.isValidEmail(mEmail)) {
			if (mEmail.length() == 0) userEmail.setError(getString(R.string.no_email_inputted));
            else if (!CustomValidator.isValidEmail(mEmail)) userEmail.setError(getString(R.string.improper_email_format));

            if (mPassword.length() == 0) userPassword.setError(getString(R.string.no_password_inputted));
		} else {
//			final ProgressDialog progressDialog = ProgressDialog.show(LoginRegisterActivity.this, "",
//						getString(R.string.progress_dialog_server_processing_request), false, true);

            progressDialog.show();

            new Thread(new Runnable() {
                @Override
                public void run() {

                    final boolean deviceRegistered = registerDevice();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {

                                if (!deviceRegistered) {
                                    Butter.down(LoginRegisterActivity.this, getString(R.string.volley_network_error));
                                    progressDialog.dismiss();
                                    return;
                                }

                                JSONObject user = new JSONObject();
                                user.put("email", mEmail)
                                    .put("password", mPassword);

                                JSONObject device = new JSONObject();
                                device.put("token", regId)
                                      .put("platform", "android");

                                JSONObject params = new JSONObject();

                                params.put("user", user)
                                      .put("device", device);

                                Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {

                                        storeSessionInformation(response);

                                        mActivityPreferences.edit()
                                                .putString(getString(R.string.last_used_email), mEmail)
                                                .apply();

                                        progressDialog.dismiss();

                                        Intent intent = new Intent(LoginRegisterActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                };

                                Response.ErrorListener errorListener = new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        progressDialog.dismiss();
                                        VolleyErrorHandler volleyError = new VolleyErrorHandler(error);

                                        Butter.between(LoginRegisterActivity.this, volleyError.getMessage());
                                    }
                                };

                                APIRequestManager.getInstance().doRequest().login(params, listener, errorListener);
                            } catch (JSONException ex) {
                                ex.printStackTrace();
                            }

                        }
                    });
                }
            }).start();
		}
	}
	
	private void processRegistration() {
		mEmail    = userEmail.getText().toString().trim();
		mPassword = userPassword.getText().toString(); //Not really one to judge if space is in password.
		mFullName = userFullName.getText().toString().trim();
	
		if (mEmail.length() == 0 || mPassword.length() == 0 || mFullName.length() == 0 || !CustomValidator.isValidEmail(mEmail)) {

            if (mEmail.length() == 0) userEmail.setError(getString(R.string.no_email_inputted));
            else if (!CustomValidator.isValidEmail(mEmail))
                userEmail.setError(getString(R.string.improper_email_format));

            if (mPassword.length() == 0)
                userPassword.setError(getString(R.string.no_password_inputted));

            if (mFullName.length() == 0)
                userFullName.setError(getString(R.string.no_name_inputted));
        } else {
            new MaterialDialog.Builder(this)
                    .title(R.string.confirm_registration_dialog_title)
                    .content(R.string.confirm_registration)
                    .positiveText(R.string.yes_caps)
                    .negativeText(R.string.no_caps)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            dialog.dismiss();

                            progressDialog.show();

                            new Thread(new Runnable() {
                                @Override
                                public void run() {

                                    final boolean deviceRegistered = registerDevice();

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {

                                                if (!deviceRegistered) {
                                                    Butter.down(LoginRegisterActivity.this, getString(R.string.volley_network_error));
                                                    progressDialog.dismiss();
                                                    return;
                                                }

                                                JSONObject user = new JSONObject();
                                                user.put("email", mEmail)
                                                        .put("password", mPassword)
                                                        .put("name", mFullName.replaceAll(RegexConstants.SPACE_NEW_LINE, " "));

                                                JSONObject device = new JSONObject();
                                                device.put("token", regId)
                                                        .put("platform", "android");

                                                JSONObject params = new JSONObject();

                                                params.put("user", user)
                                                        .put("device", device);

                                                Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                                                    @Override
                                                    public void onResponse(JSONObject response) {

                                                        storeSessionInformation(response);
                                                        String nameOnPhone = mActivityPreferences.getString(getString(R.string.name_on_phone), "");
                                                        SharedPreferences.Editor editor = mActivityPreferences.edit();

                                                        if (mFullName.equals(nameOnPhone)) {
                                                            editor.putBoolean(getString(R.string.used_name_on_phone), true);
                                                        }

                                                        editor.putString(getString(R.string.last_used_email), mEmail);
                                                        editor.apply();

                                                        progressDialog.dismiss();

                                                        Intent intent = new Intent(LoginRegisterActivity.this, MainActivity.class);
                                                        startActivity(intent);
                                                        finish();
                                                    }
                                                };

                                                Response.ErrorListener errorListener = new Response.ErrorListener() {
                                                    @Override
                                                    public void onErrorResponse(VolleyError error) {
                                                        progressDialog.dismiss();
                                                        VolleyErrorHandler volleyError = new VolleyErrorHandler(error);

                                                        Butter.between(LoginRegisterActivity.this, volleyError.getMessage());
                                                    }
                                                };

                                                APIRequestManager.getInstance().doRequest().register(params, listener, errorListener);
                                            } catch (JSONException ex) {
                                                ex.printStackTrace();
                                            }

                                        }
                                    });
                                }
                            }).start();
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            dialog.dismiss();

                            Butter.between(LoginRegisterActivity.this, "A robot just discovered what it feels like to be sad :(");
                        }
                    }).show();
		}
	}	
	
	private void processForgotPassword() {
		mEmail = userEmail.getText().toString();
		
		if (mEmail.length() == 0) userEmail.setError(getString(R.string.no_email_inputted));
		else if(!CustomValidator.isValidEmail(mEmail)) userEmail.setError(getString(R.string.improper_email_format));
		else {
			try {
                progressDialog.show();

                JSONObject params = new JSONObject();
			
				params.put("email", mEmail);

                Response.Listener<Integer> listener = new Response.Listener<Integer>() {
					@Override
					public void onResponse(Integer response) {
												
						progressDialog.dismiss();

                        Butter.between(LoginRegisterActivity.this, getString(R.string.forgotton_password_email_sent));
					}
				};
			
				Response.ErrorListener errorListener = new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						progressDialog.dismiss();
						VolleyErrorHandler volleyError = new VolleyErrorHandler(error);

                        Butter.between(LoginRegisterActivity.this, volleyError.getMessage());
					}
				};
			
				APIRequestManager.getInstance().doRequest().sendForgottonPasswordEmail(params, listener, errorListener);
			} catch (JSONException ex) {
				ex.printStackTrace();
			}
			
		}
	}
	
	private void storeSessionInformation(JSONObject response) {
        Log.v("response...", response.toString());
		SharedPreferences.Editor editor = mSessionPreferences.edit();
		try {
			JSONObject userData = response.getJSONObject("user");
			editor.putString(getString(R.string.user_auth_token_key), userData.getString("auth_token"))
				  .putString(getString(R.string.user_id_key),         userData.getString("external_id"))
				  .putString(getString(R.string.user_email_key),      userData.getString("email"))
				  .putString(getString(R.string.user_name_key),       userData.getString("name"))
				  .putString(getString(R.string.user_created_at_key), userData.getString("created_at"))
				  .apply();
			
		} catch (JSONException ex) {
			ex.printStackTrace();
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == EMAIL_REQUEST_INTENT && resultCode == RESULT_OK) {
			mEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            if (!mEmail.isEmpty()) {
                userEmail.setText(mEmail);
            }
		}
	}

	private void getAndSetEmail() {
		mEmail = mActivityPreferences.getString(
				getString(R.string.last_used_email), null);

		if (mEmail == null) {
            // Try AccountPicker
			try {
				Intent intent = AccountPicker.newChooseAccountIntent(null, null,
						new String[] { GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE }, false, null, null, null, null);
				startActivityForResult(intent, EMAIL_REQUEST_INTENT);
			} catch (ActivityNotFoundException e) {
				e.printStackTrace();
			}
		} else {
            userEmail.setText(mEmail);
        }
	}

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST, new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {

                                // They MUST have Google Play Services.
                                // This prevents them pressing back in the dialog
                                // and going along their merry way.
                                checkPlayServices();
                            }
                        }).show();
            } else {
                Butter.between(this, "This device is not supported because it can't access the Google Play Store.");
                finish();
            }

            return false;
      }

        return true;
    }

    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences();
        String registrationId = prefs.getString(PROPERTY_REG_ID, null);
        if (registrationId == null) {
            return null;
        }

        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            return null;
        }

        return registrationId;
    }

    private SharedPreferences getGCMPreferences() {
        return getSharedPreferences(getString(R.string.gcm_preferences_key), MODE_PRIVATE);
    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);

            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    private void storeRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences();
        int appVersion = getAppVersion(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.apply();
    }

    // Register device for Google Cloud Messaging
    // Note: gcm.register(Sender_ID) is a blocking method.
    private boolean registerDevice() {
        boolean gcmRequestSucceeded = true;

        if (gcm == null)
            gcm = GoogleCloudMessaging.getInstance(this);

        regId = getRegistrationId(this);
        if (regId == null) {
            try {
                regId = gcm.register(SENDER_ID);
                storeRegistrationId(this);
            } catch (IOException io) {
                gcmRequestSucceeded = false;
            }
        }

        return gcmRequestSucceeded;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("viewState", viewState.name());
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.login_register, menu);
		return true;
	}
}
