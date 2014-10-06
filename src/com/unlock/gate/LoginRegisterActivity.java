package com.unlock.gate;

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

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;

public class LoginRegisterActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {
	
	private enum State {
		LOGIN, REGISTRATION, FORGOT_PASSWORD
	}
	
	private final static String LOGIN_API_ENDPOINT = "sessions.json";
	private final static String REGISTER_API_ENDPOINT = "registration.json";
	private final int EMAIL_REQUEST_INTENT = 1;
	
	private SharedPreferences mPreferences;
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
		
		mPreferences = getSharedPreferences(
				getString(R.string.login_register_shared_preferences_key), MODE_PRIVATE);
		
		instantiateViews();
		setState(State.LOGIN);
		
		getAndSetEmail();
		getAndSetFullName();
		
		handleForgotPassword();
		handleTerms();
		handleToggleLoginRegistration();
		handleCommandButton();
		
	}
	
	public void instantiateViews() {
		userEmail    = (EditText) findViewById(R.id.userEmail);
		userPassword = (EditText) findViewById(R.id.userPassword);
		userFullName = (EditText) findViewById(R.id.userFullName);
		
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
				setState(State.FORGOT_PASSWORD);
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
				
				if (loginViewFlag) {
					forgotPassword.setVisibility(View.INVISIBLE);
					Fade.show(userFullName);
					
					commandButton.setText(R.string.register);
					toggleRegistrationLogin.setText(R.string.toggle_login);
					loginViewFlag = false;
				} else {
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
					loginViewFlag = true;
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
			}
		});
	}
	
	public void setState(State state) {
		viewState = state;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == EMAIL_REQUEST_INTENT && resultCode == RESULT_OK) {
			mEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
			userEmail.setText(mEmail);
		}
	}
	
	public void getAndSetEmail() {
		mEmail = mPreferences.getString(
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
