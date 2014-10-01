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

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;

public class LoginRegisterActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {

	// Check prefs for what email to use. If not there, accountpicker, if not there, blank.
	// Make sure to store last successfully used email in the prefs.
	// check profile for full name. If not there, blank.
	
	private final static String LOGIN_API_ENDPOINT = "sessions.json";
	private final static String REGISTER_API_ENDPOINT = "registration.json";
	private final int EMAIL_REQUEST_INTENT = 1;
	
	private SharedPreferences mPreferences;
	private String mEmail;
	private String mPassword;
	private String mFullName;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login_register);
		
		mPreferences = getSharedPreferences(
				getString(R.string.login_register_shared_preferences_key), MODE_PRIVATE);
		
		getEmailAndFullName();		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == EMAIL_REQUEST_INTENT && resultCode == RESULT_OK) {
			mEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
		}
	}
	
	public void getEmailAndFullName() {
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
