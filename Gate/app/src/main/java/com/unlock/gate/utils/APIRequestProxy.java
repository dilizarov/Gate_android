package com.unlock.gate.utils;

import android.content.Context;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class APIRequestProxy {
	
	private final String BASE_URL = "http://infinite-river-7560.herokuapp.com/api/v1/";
	
	private final String SESSION_ENDPOINT = "sessions.json";
	private final String REGISTRATION_ENDPOINT = "registrations.json";
    private final String NETWORKS_ENDPOINT = "networks.json";
	
	private RequestQueue mRequestQueue;
	
	APIRequestProxy(Context context) {
		mRequestQueue = Volley.newRequestQueue(context.getApplicationContext());
	}
	
	private String getAbsoluteUrl(String endpoint) {
		return BASE_URL + endpoint;
	}
	
	public void login(JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
		JsonObjectRequest request = new JsonObjectRequest(Method.POST, getAbsoluteUrl(SESSION_ENDPOINT), params, listener, errorListener);
		
		mRequestQueue.add(request);
	}
	
	public void register(JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
		JsonObjectRequest request = new JsonObjectRequest(Method.POST, getAbsoluteUrl(REGISTRATION_ENDPOINT), params, listener, errorListener);
	
		mRequestQueue.add(request);
	}

	public void sendForgottonPasswordEmail(JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
		//TODO: Write this function and the associated server code required to pull it off
	}

    public void getNetworks(JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        //Sadly, Volley does not offer a very fluid experience for get requests with params.
        String url = getAbsoluteUrl(NETWORKS_ENDPOINT);
        StringBuilder buildUrl = new StringBuilder(url);
        buildUrl.append("?")
                .append("user_id=")
                .append(params.optString("user_id"))
                .append("&")
                .append("auth_token=")
                .append(params.optString("auth_token"));

        JsonObjectRequest request = new JsonObjectRequest(Method.GET, buildUrl.toString(), null, listener, errorListener);

        mRequestQueue.add(request);
    }
}
