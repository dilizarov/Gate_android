package com.unlock.gate.utils;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

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

        deriveStatusCode();

        if(error instanceof NetworkError || error instanceof TimeoutError) {
            mErrorType = error instanceof NetworkError
                         ? Error.NETWORK
                         : Error.TIMEOUT;

            mMessage = MyApplication.getContext().getString(R.string.volley_network_error);
        } else if((error instanceof ServerError && mStatusCode >= 500) ||
                   error instanceof ParseError) {
            // Volley Documentation is incorrect. ServerError is supposed to
            // be used for only 5xx errors, but it encompasses every error but a select few.
            mErrorType = error instanceof ServerError
                    ? Error.SERVER
                    : Error.PARSE;

            mMessage = MyApplication.getContext().getString(R.string.volley_common_error);
        } else if(error instanceof AuthFailureError) {
            mErrorType = Error.AUTH;
            mMessage = MyApplication.getContext().getString(R.string.volley_auth_error);
        } else if(error instanceof NoConnectionError) {
            mErrorType = Error.NOCONNECTION;
            mMessage = MyApplication.getContext().getString(R.string.volley_no_connection_error);
        } else {
            mErrorType = Error.EXPECTED;
            deriveErrorData();
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

    public boolean isConnectionError() {
        return mErrorType == Error.NETWORK ||
               mErrorType == Error.TIMEOUT ||
               mErrorType == Error.NOCONNECTION;
    }

	public boolean isExpectedError() {
		return mErrorType == Error.EXPECTED;
	}
	
	private boolean existsNetworkResponse() {
		return (mError.networkResponse != null);
	}
	
	private void deriveStatusCode() {
        if (mError.networkResponse != null && mError.networkResponse.statusCode > 0) {
            mStatusCode = mError.networkResponse.statusCode;
        } else {
            // The request didn't even leave the phone. Any positive Status Code
            // implies that the request is coming back from the server.
            mStatusCode = -1;
        }
	}
	
	private void deriveErrorData() {
		if (existsNetworkResponse() && mError.networkResponse.data == null) {
            mJSONErrors = null;
            mMessage = MyApplication.getContext().getString(R.string.volley_unknown_error);
            return;
        }
		
		try {
			String jsonString = new String(mError.networkResponse.data, "utf-8");
			mJSONErrors = new JSONObject(jsonString);

            JSONArray errorsArray = mJSONErrors.optJSONArray("errors");

            StringBuilder errorString = new StringBuilder();

            int j = errorsArray.length();
            for (int i = 0; i < j; i++) {
                if (i != 0) errorString.append("\n");
                errorString.append(errorsArray.optString(i));
            }

            mMessage = errorString.toString();

		} catch (UnsupportedEncodingException uee) {
			Log.v("String Encoding Error", "Problem converting error data from server to utf-8. Talk to Rails people.");
		} catch (JSONException ex) {
			ex.printStackTrace();
		}
	}
}
