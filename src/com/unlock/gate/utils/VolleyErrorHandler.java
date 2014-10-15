package com.unlock.gate.utils;

import java.io.UnsupportedEncodingException;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.unlock.gate.MyApplication;
import com.unlock.gate.R;

public class VolleyErrorHandler {

	public enum Error {
		NETWORK, SERVER, AUTH, EXPECTED,
		PARSE, NOCONNECTION, TIMEOUT
	}
	
	private Error mErrorType;
	private VolleyError mError;
	private int mStatusCode;
	private String mMessage;
	private JSONObject mJSONErrors;
	
	public VolleyErrorHandler(VolleyError error) {
		this.mError = error;
		if (existsNetworkResponse()) {
			mErrorType = Error.EXPECTED;
			deriveStatusCode();
			deriveErrorData();
		} else {
			if( error instanceof NetworkError) {
				mErrorType = Error.NETWORK;
				mMessage = MyApplication.getContext().getString(R.string.volley_network_error);
			} else if( error instanceof ServerError) {
				mErrorType = Error.SERVER;
				mMessage = MyApplication.getContext().getString(R.string.volley_server_error);
			} else if( error instanceof AuthFailureError) {
				mErrorType = Error.AUTH;
				mMessage = MyApplication.getContext().getString(R.string.volley_auth_error);
			} else if( error instanceof ParseError) {
				mErrorType = Error.PARSE;
				mMessage = MyApplication.getContext().getString(R.string.volley_parse_error);
			} else if( error instanceof NoConnectionError) {
				mErrorType = Error.NOCONNECTION;
				mMessage = MyApplication.getContext().getString(R.string.volley_no_connection_error);
			} else if( error instanceof TimeoutError) {
				mErrorType = Error.TIMEOUT;
				mMessage = MyApplication.getContext().getString(R.string.volley_timeout_error);
			}
		}
	}
	
	public Error getErrorType() {
		return mErrorType;
	}
	
	public String getMessage() {
		return mMessage;
	}
	
	public int getStatusCode() {
		return mStatusCode;
	}
	
	public JSONObject getErrors() {
		return mJSONErrors;
	}
	public boolean isExpectedError() {
		return mErrorType == Error.EXPECTED;
	}
	
	private boolean existsNetworkResponse() {
		return (mError.networkResponse != null);
	}
	
	private void deriveStatusCode() {
		mStatusCode = mError.networkResponse.statusCode;
	}
	
	private void deriveErrorData() {
		if (mError.networkResponse.data == null) mJSONErrors = null;
		
		try {
			String jsonString = new String(mError.networkResponse.data, "utf-8");
			Log.d("JSON Info", jsonString);
			mJSONErrors = new JSONObject(jsonString);
		} catch (UnsupportedEncodingException uee) {
			
		} catch (JSONException ex) {
			ex.printStackTrace();
		}
	}
}
