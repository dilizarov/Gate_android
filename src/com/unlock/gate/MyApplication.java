package com.unlock.gate;

import com.unlock.gate.utils.APIRequestManager;

import android.app.Application;

public class MyApplication extends Application {
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		APIRequestManager.getInstance(getApplicationContext());
	}
}
