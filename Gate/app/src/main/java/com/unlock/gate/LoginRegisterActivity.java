package com.unlock.gate;

import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.unlock.gate.utils.APIRequestManager;
import com.unlock.gate.utils.Butter;
import com.unlock.gate.utils.CustomValidator;
import com.unlock.gate.utils.Fade;
import com.unlock.gate.utils.RegexConstants;
import com.unlock.gate.utils.SetErrorBugFixer;
import com.unlock.gate.utils.VolleyErrorHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import de.keyboardsurfer.android.widget.crouton.Configuration;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class LoginRegisterActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {
	
	private enum State {
		LOGIN, REGISTRATION, FORGOT_PASSWORD
	}

	private final int EMAIL_REQUEST_INTENT = 1;

	private SharedPreferences mActivityPreferences;
	private SharedPreferences mSessionPreferences;
	
	private String mEmail;
	private String mPassword;
	private String mFullName;
	
	private EditText userEmail;
	private EditText userPassword;
	private EditText userFullName;
	
	private TextView forgotPassword;
	private TextView terms;
	private TextView toggleRegistrationLogin;
	
	private Button commandButton;
	
	private State viewState;

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private GoogleCloudMessaging gcm;
    private String regId;
    private final static String PROPERTY_REG_ID = "registration_id";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login_register);
		
		mActivityPreferences = getSharedPreferences(
				getString(R.string.login_register_shared_preferences_key), MODE_PRIVATE);
		
		mSessionPreferences  = getSharedPreferences(
				getString(R.string.session_shared_preferences_key), MODE_PRIVATE); 
		
		viewState = State.LOGIN;

        if (mSessionPreferences.contains(getString(R.string.user_auth_token_key))) {
            Intent intent = new Intent(LoginRegisterActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            instantiateViews();

            getAndSetEmail();
            getAndSetFullName();

            //handling sets up event listeners and actions.
            //Only handleCommandButton actually communicates to the server
            handleForgotPassword();
            handleTerms();
            handleToggleLoginRegistration();
            handleCommandButton();
        }

	}

    @Override
    protected void onResume() {
        super.onResume();
    }
	
	private void instantiateViews() {
		userEmail    = (EditText) findViewById(R.id.userEmail);
		userPassword = (EditText) findViewById(R.id.userPassword);
		userFullName = (EditText) findViewById(R.id.userFullName);
		
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
					public void onAnimationEnd(Animator animation) {
						userPassword.setAlpha(1);
						userPassword.setVisibility(View.INVISIBLE);
						userPassword.animate().setListener(null);
					}
					
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
				//TODO
				Log.v("Click terms", "Look, you're clicking me!");
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
								userFullName.setAlpha(1);
								userFullName.setVisibility(View.INVISIBLE);
								userFullName.animate().setListener(null);
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
		
		if (mEmail.length() == 0 || mPassword.length() == 0) {
			if (mEmail.length() == 0)    userEmail.setError(getString(R.string.no_email_inputted));
			if (mPassword.length() == 0) userPassword.setError(getString(R.string.no_password_inputted));
		} else if (!CustomValidator.isValidEmail(mEmail)) userEmail.setError(getString(R.string.improper_email_format));
		else {
			final ProgressDialog progressDialog = ProgressDialog.show(LoginRegisterActivity.this, "",
						getString(R.string.progress_dialog_server_processing_request), false, true);

            new Thread(new Runnable() {
                @Override
                public void run() {

                    final Context context = LoginRegisterActivity.this;
                    boolean gcmRequestFailed = false;
                    if (gcm == null)
                        gcm = GoogleCloudMessaging.getInstance(context);

                    regId = getRegistrationId(context);
                    if (regId == null) {
                        try {
                            regId = gcm.register("222761912510");
                            storeRegistrationId(context);
                            Log.v("RegID", regId);
                        } catch (IOException io) {
                            gcmRequestFailed = true;
                        }
                    }

                    final boolean requestFailed = gcmRequestFailed;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {

                                if (requestFailed) {
                                    Butter.down(context, "INTERNET WELPED");
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
                                                .commit();

                                        progressDialog.dismiss();
                                        Crouton.showText(LoginRegisterActivity.this, response.toString(), Style.CONFIRM);
                                        Log.d("Correct stuff", response.toString());

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

                                        if (volleyError.isExpectedError()) {
                                            JSONObject errorsJSON = volleyError.getErrors();

                                            JSONArray errorsArray = errorsJSON.optJSONArray("errors");
                                            String incorrectEmailPassword = errorsArray.optString(0);

                                            Crouton.makeText(LoginRegisterActivity.this, incorrectEmailPassword, Style.ALERT)
                                                    .setConfiguration(new Configuration.Builder().setDuration(Configuration.DURATION_LONG).build())
                                                    .show();
                                        } else {
                                            Crouton.makeText(LoginRegisterActivity.this, volleyError.getMessage(), Style.ALERT)
                                                    .setConfiguration(new Configuration.Builder().setDuration(Configuration.DURATION_LONG).build())
                                                    .show();
                                        }
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
	
		if (mEmail.length() == 0 || mPassword.length() == 0 || mFullName.length() == 0) {
			if (mEmail.length() == 0)    userEmail.setError(getString(R.string.no_email_inputted));
			if (mPassword.length() == 0) userPassword.setError(getString(R.string.no_password_inputted));
			if (mFullName.length() == 0) userFullName.setError(getString(R.string.no_name_inputted));
		} else if (!CustomValidator.isValidEmail(mEmail)) userEmail.setError(getString(R.string.improper_email_format));
		else {

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getString(R.string.confirm_registration_dialog_title))
					.setMessage(getString(R.string.confirm_registration))
					.setNegativeButton(getString(R.string.no_caps), new DialogInterface.OnClickListener() {
				
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();				
						}
					})
			
					.setPositiveButton(getString(R.string.yes_caps), new DialogInterface.OnClickListener() {
				
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();

                            final ProgressDialog progressDialog = ProgressDialog.show(LoginRegisterActivity.this, "",
                                    getString(R.string.progress_dialog_server_processing_request), false, true);

                            new Thread(new Runnable() {
                                @Override
                                public void run() {

                                    final Context context = LoginRegisterActivity.this;
                                    boolean gcmRequestFailed = false;
                                    if (gcm == null)
                                        gcm = GoogleCloudMessaging.getInstance(context);

                                    regId = getRegistrationId(context);
                                    if (regId == null) {
                                        try {
                                            regId = gcm.register("222761912510");
                                            storeRegistrationId(context);
                                            Log.v("RegID", regId);
                                        } catch (IOException io) {
                                            gcmRequestFailed = true;
                                        }
                                    }

                                    final boolean requestFailed = gcmRequestFailed;

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {

                                                if (requestFailed) {
                                                    Butter.down(context, "INTERNET WELPED");
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
                                                        editor.commit();

                                                        progressDialog.dismiss();
                                                        Crouton.showText(LoginRegisterActivity.this, response.toString(), Style.CONFIRM);
                                                        Log.d("Correct stuff", response.toString());

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

                                                        if (volleyError.isExpectedError()) {
                                                            Crouton.makeText(LoginRegisterActivity.this, volleyError.getPrettyErrors(), Style.ALERT)
                                                                    .setConfiguration(new Configuration.Builder().setDuration(Configuration.DURATION_LONG).build())
                                                                    .show();
                                                        } else {
                                                            Crouton.makeText(LoginRegisterActivity.this, volleyError.getMessage(), Style.ALERT)
                                                                    .setConfiguration(new Configuration.Builder().setDuration(Configuration.DURATION_LONG).build())
                                                                    .show();
                                                        }
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
					});
			
			AlertDialog confirmation = builder.create();
			confirmation.show();
		}
	}	
	
	private void processForgotPassword() {
		mEmail = userEmail.getText().toString();
		
		if (mEmail.length() == 0) userEmail.setError(getString(R.string.no_email_inputted));
		else if(!CustomValidator.isValidEmail(mEmail)) userEmail.setError(getString(R.string.improper_email_format));
		else {
			try {
				final ProgressDialog progressDialog = ProgressDialog.show(LoginRegisterActivity.this, "", 
						getString(R.string.progress_dialog_server_processing_request), false, true);
			
				JSONObject params = new JSONObject();
			
				params.put("email", mEmail);

                Response.Listener<Integer> listener = new Response.Listener<Integer>() {
					@Override
					public void onResponse(Integer response) {
												
						progressDialog.dismiss();
						
						Crouton.makeText(LoginRegisterActivity.this, getString(R.string.forgotton_password_email_sent), Style.CONFIRM)
								.setConfiguration(new Configuration.Builder().setDuration(Configuration.DURATION_LONG).build())
								.show();
					}
				};
			
				Response.ErrorListener errorListener = new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						progressDialog.dismiss();
						VolleyErrorHandler volleyError = new VolleyErrorHandler(error);
						
						if (volleyError.isExpectedError()) {
							Crouton.makeText(LoginRegisterActivity.this, getString(R.string.forgotton_password_email_not_registered), Style.ALERT)
									.setConfiguration(new Configuration.Builder().setDuration(Configuration.DURATION_LONG).build())
									.show();
						} else {
							Crouton.makeText(LoginRegisterActivity.this, volleyError.getMessage(), Style.ALERT)
									.setConfiguration(new Configuration.Builder().setDuration(Configuration.DURATION_LONG).build())
									.show();
						}
					}	
				};
			
				APIRequestManager.getInstance().doRequest().sendForgottonPasswordEmail(params, listener, errorListener);
			} catch (JSONException ex) {
				ex.printStackTrace();
			}
			
		}
	}
	
	private void storeSessionInformation(JSONObject response) {
		SharedPreferences.Editor editor = mSessionPreferences.edit();
		try {
			JSONObject userData = response.getJSONObject("user");
			editor.putString(getString(R.string.user_auth_token_key), userData.getString("auth_token"))
				  .putString(getString(R.string.user_id_key),         userData.getString("external_id"))
				  .putString(getString(R.string.user_email_key),      userData.getString("email"))
				  .putString(getString(R.string.user_name_key),       userData.getString("name"))
				  .putString(getString(R.string.user_created_at_key), userData.getString("created_at"))
				  .commit();
			
		} catch (JSONException ex) {
			ex.printStackTrace();
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == EMAIL_REQUEST_INTENT && resultCode == RESULT_OK) {
			mEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            if (!mEmail.isEmpty()) userEmail.setText(mEmail);
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
				Log.v("EMAIL_REQUEST_INTENT", "Activity was not found");
			}
		} else {
            userEmail.setText(mEmail);
        }
	}
	
	private void getAndSetFullName() {
		// Use ContactContracts.Profile to try to get full name
        if (!mActivityPreferences.getBoolean(getString(R.string.used_name_on_phone), false))
            getLoaderManager().initLoader(0, null, this);
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle arguments) {
		return new CursorLoader(
						this, 
						Uri.withAppendedPath(
								ContactsContract.Profile.CONTENT_URI, 
								ContactsContract.Contacts.Data.CONTENT_DIRECTORY),
						new String[]{ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME},
						ContactsContract.Contacts.Data.MIMETYPE + " = ?",
						new String[]{ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE},
						null
								);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
		cursor.moveToFirst();
        if (!cursor.isNull(0)) {
            mFullName = cursor.getString(0);
            if (!mFullName.isEmpty()) {
                userFullName.setText(mFullName);
                mActivityPreferences.edit()
                                    .putString(getString(R.string.name_on_phone), mFullName)
                                    .commit();
            }
        }
	}
	
	@Override
	public void onLoaderReset(Loader<Cursor> cursorLoader) {
	}

//    private boolean checkPlayServices() {
//        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
//        if (resultCode != ConnectionResult.SUCCESS) {
//            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
//                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
//                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
//            } else {
//                Log.i("PlayServices", "This device is not supported.");
//                finish();
//            }
//
//            return false;
//        }
//
//        return true;
//    }

    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, null);
        if (registrationId == null) {
            Log.i("RegistrationId", "Registration not found");
            return null;
        }

        int registeredVersion = prefs.getInt("Registered_APP_VERSION", Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i("RegistrationId", "Registration not found");
            return null;
        }

        return registrationId;
    }

    private SharedPreferences getGCMPreferences(Context context) {
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
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt("Property_APP_VERSION", appVersion);
        editor.commit();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.login_register, menu);
		return true;
	}
}
