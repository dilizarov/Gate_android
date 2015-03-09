package com.unlock.gate.utils;

import android.content.Context;

public class APIRequestManager {
	
	private static APIRequestManager instance;
	private APIRequestProxy mRequestProxy;
	
	private APIRequestManager(Context context) {
		mRequestProxy = new APIRequestProxy(context);
	}
	
	public APIRequestProxy doRequest() {
		return mRequestProxy;
	}

    public void cancelAllFeedRequests() {
        mRequestProxy.cancelAllFeedRequests();
    }

    public void cancelAllGeneratedGatesRequests() {
        mRequestProxy.cancelAllGeneratedGatesRequests();
    }

	// This method should be called first to do singleton initialization
	public static synchronized APIRequestManager getInstance(Context context) {
		if (instance == null) {
			instance = new APIRequestManager(context);
		}
		
		return instance;
	}
	
	public static synchronized APIRequestManager getInstance() {
		if (instance == null) {
			throw new IllegalStateException(APIRequestManager.class.getSimpleName() + 
					" is not initialized, call getInstance(..) method first.");
		}
		
		return instance;
	}
	
	
}
