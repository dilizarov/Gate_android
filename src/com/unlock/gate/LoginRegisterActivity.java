package com.unlock.gate;

import android.accounts.AccountManager;
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
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;

public class LoginRegisterActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {
	
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
	
	private boolean loginViewFlag = true;
	private AlphaAnimation fadeIn;
	private AlphaAnimation fadeOut;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login_register);
		
		mPreferences = getSharedPreferences(
				getString(R.string.login_register_shared_preferences_key), MODE_PRIVATE);
		
		instantiateViews();
		instantiateAnimations();
		
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
	
	public void instantiateAnimations() {
		fadeIn = new AlphaAnimation(0.0f, 1.0f);
		fadeIn.setDuration(1000);
		
		fadeOut = new AlphaAnimation(1.0f, 0.0f);
		fadeOut.setDuration(1000);
	}
	
	public void handleForgotPassword() {
		forgotPassword.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.v("Click forgot password", "Look, you're clicking me");
				//Fade out password bar.
				//Change command to send
				//Change bottom left to toggle to login
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
				////invisible forgot password
				////Fade in Full name
				////Change command to register
				////Change bottom left to toggle login
				//ELSE IF ON REGISTER
				////fade out Full name
				////visible forgot password
				////change command to login
				////change button left to toggle register
				
				if (loginViewFlag) {
					forgotPassword.setVisibility(android.view.View.INVISIBLE);
					userFullName.startAnimation(fadeIn);
					userFullName.setVisibility(android.view.View.VISIBLE);
					commandButton.setText(R.string.register);
					toggleRegistrationLogin.setText(R.string.toggle_login);
					loginViewFlag = false;
				} else {
					
					AnimationListener listener = new AnimationListener() {
						
						@Override
						public void onAnimationEnd(Animation animation) {
							forgotPassword.setVisibility(android.view.View.VISIBLE);
							userFullName.setVisibility(android.view.View.INVISIBLE);
						}
						
						@Override 
						public void onAnimationRepeat(Animation animation) {
						}
						
						@Override
						public void onAnimationStart(Animation animation){
						}
						
					};
					
					fadeOut.setAnimationListener(listener);
					userFullName.startAnimation(fadeOut);
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
