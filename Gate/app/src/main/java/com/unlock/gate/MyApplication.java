package com.unlock.gate;

import android.app.Application;
import android.content.Context;

import com.unlock.gate.utils.APIRequestManager;

public class MyApplication extends Application {
	
	private static Context mContext;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		mContext = this;
		APIRequestManager.getInstance(getApplicationContext());
	}
	
	public static Context getContext() {
		return mContext;
	}
}
