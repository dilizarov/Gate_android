package com.unlock.gate;

import android.app.Application;
import android.content.Context;

import com.unlock.gate.models.Gate;
import com.unlock.gate.utils.APIRequestManager;

import java.util.ArrayList;

public class MyApplication extends Application {
	
	private static Context mContext;

    private static ArrayList<Gate> updatedGates;

	@Override
	public void onCreate() {
		super.onCreate();
		
		mContext = this;
        updatedGates = new ArrayList<Gate>();
		APIRequestManager.getInstance(getApplicationContext());
	}
	
	public static Context getContext() {
		return mContext;
	}

    public static void setUpdatedGates(ArrayList<Gate> gates) {
        updatedGates = gates;
    }

    public static ArrayList<Gate> getUpdatedGates() {
        return updatedGates;
    }
}
