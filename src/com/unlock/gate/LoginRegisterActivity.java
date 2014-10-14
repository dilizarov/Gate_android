package com.unlock.gate;

import java.io.UnsupportedEncodingException;

import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
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
import com.unlock.gate.utils.APIRequestManager;
import com.unlock.gate.utils.CustomValidator;
import com.unlock.gate.utils.Fade;
import com.unlock.gate.utils.SetErrorBugFixer;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class LoginRegisterActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {
	
	private enum State {
		LOGIN, REGISTRATION, FORGOT_PASSWORD
	}
	
	private final static String LOGIN_API_ENDPOINT           = "sessions.json";
	private final static String REGISTER_API_ENDPOINT        = "registration.json";
	//Placeholder route
	private final static String FORGOT_PASSWORD_API_ENDPOINT = "password_reset.json"; 
	
	private final int EMAIL_REQUEST_INTENT = 1;
	
	private SharedPreferences mActivityPreferences;
	private SharedPreferences mUserPreferences;
	
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
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login_register);
		
		mActivityPreferences = getSharedPreferences(
				getString(R.string.login_register_shared_preferences_key), MODE_PRIVATE);
		
		mUserPreferences     = getSharedPreferences(
				getString(R.string.user_shared_preferences_key), MODE_PRIVATE); 
		
		viewState = State.LOGIN;
		
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
	
	public void instantiateViews() {
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
	
	public void handleForgotPassword() {
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
				});
				
				commandButton.setText(R.string.send_email);
				toggleRegistrationLogin.setText(R.string.toggle_login);
				viewState = State.FORGOT_PASSWORD;
			}
		});
	}
	
	public void handleTerms() {
		terms.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//TODO
				Log.v("Click terms", "Look, you're clicking me!");
			}
		});
	}
	
	public void handleToggleLoginRegistration() {
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
					Fade.show(userFullName);
					
					commandButton.setText(R.string.register);
					toggleRegistrationLogin.setText(R.string.toggle_login);
					viewState = State.REGISTRATION;
				} else {
					//The true actions shouldn't even be possible, not sure why I put this here
					if (userPassword.getVisibility() == View.INVISIBLE) {
						Fade.show(userPassword);
						forgotPassword.setVisibility(View.VISIBLE);
					} else {
						Fade.hide(userFullName, new AnimatorListenerAdapter() {
							public void onAnimationEnd(Animator animation) {
								userFullName.setAlpha(1);
								userFullName.setVisibility(View.INVISIBLE);
								userFullName.animate().setListener(null);
								forgotPassword.setVisibility(View.VISIBLE);
							}
						});
					}
					
					commandButton.setText(R.string.log_in);
					toggleRegistrationLogin.setText(R.string.toggle_registration);
					viewState = State.LOGIN;
				}
			}
		});
	}
	
	public void handleCommandButton() {
		commandButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				//if send, send email
				//if login go to that
				//if register go to that
			
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
	
	public void processLogin() {
		mEmail    = userEmail.getText().toString();
		mPassword = userPassword.getText().toString();
		
		if (mEmail.length() == 0 || mPassword.length() == 0) {
			if (mEmail.length() == 0) userEmail.setError(getString(R.string.no_email_inputted));
			if (mPassword.length() == 0) userPassword.setError(getString(R.string.no_password_inputted));
		} else if (!CustomValidator.isValidEmail(mEmail)) userEmail.setError(getString(R.string.improper_email_format));
		else {
			try {
				JSONObject user = new JSONObject();
				user.put("email", mEmail)
					.put("password", mPassword);
			
			
				JSONObject params = new JSONObject();
			
				params.put("user", user);
			
				Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						//Crouton.showText(LoginRegisterActivity.this, response.toString(), Style.CONFIRM);
						//Log.d("Correct stuff", response.toString());
					}
				};
			
				Response.ErrorListener errorListener = new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						if (error.networkResponse != null) {
//							try {
//								String responseBody = new String(error.networkResponse.data, "utf-8");
//								Crouton.showText(LoginRegisterActivity.this, responseBody, Style.ALERT);
//								Log.d("Wrong stuff", error.networkResponse.data.toString());
//							} catch (UnsupportedEncodingException uee) {}
						}
					}	
				};
			
				APIRequestManager.getInstance().doRequest().login(params,listener, errorListener);
			} catch (JSONException ex) {
				ex.printStackTrace();
			}
			
			
//			NetworkClient.post(LOGIN_API_ENDPOINT, params, new JsonHttpResponseHandler() {
//				@Override
//				public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
//					try {
//						//SharedPreferences.Editor editor = mUserPreferences.edit();
//						//editor.putString(getString(R.string.user_auth_token),
//						//		response.getJSONObject("info").getString("auth_token"));
//						
//						Toast.makeText(LoginRegisterActivity.this, "Woot", Toast.LENGTH_LONG).show();
//						
//						//Crouton.showText(LoginRegisterActivity.this, "Success!", Style.CONFIRM);
//						//Here, we do something...
//					} catch (Exception e) {
//						//Crouton.showText(LoginRegisterActivity.this, "Welp", Style.ALERT);
//					}
//				}
//				
//				@Override
//				public void onFailure(int statusCode, Header[] headers, Throwable thrown, JSONObject errorResponse) {
//					Crouton.showText(LoginRegisterActivity.this, "Welp", Style.ALERT);
//					//Toast.makeText(LoginRegisterActivity.this, "Welp", Toast.LENGTH_LONG).show();
//				}
//			});
		}
	}
	
	public void processRegistration() {
		mEmail    = userEmail.getText().toString();
		mPassword = userPassword.getText().toString();
		mFullName = userFullName.getText().toString();
	
		if (mEmail.length() == 0 || mPassword.length() == 0 || mFullName.length() == 0) {
			if (mEmail.length() == 0) userEmail.setError("Forgot this");
			if (mPassword.length() == 0) userPassword.setError("Forgot this");
			if (mFullName.length() == 0) userFullName.setError("Forgot this");
		}
	}
	
	public void processForgotPassword() {
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == EMAIL_REQUEST_INTENT && resultCode == RESULT_OK) {
			mEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
			userEmail.setText(mEmail);
		}
	}
	
	public void getAndSetEmail() {
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
		}
	}
	
	public void getAndSetFullName() {
		// Use ContactContracts.Profile to try to get full name
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
		mFullName = cursor.getString(0);
		userFullName.setText(mFullName);
	}
	
	@Override
	public void onLoaderReset(Loader<Cursor> cursorLoader) {
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.login_register, menu);
		return true;
	}
}
