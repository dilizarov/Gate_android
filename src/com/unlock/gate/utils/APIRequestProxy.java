package com.unlock.gate.utils;

import org.json.JSONObject;

import android.content.Context;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

public class APIRequestProxy {
	
	private final String BASE_URL = "http://infinite-river-7560.herokuapp.com/api/v1/";
	
	private final String SESSION_ENDPOINT = "sessions.json";
	private final String REGISTRATION_ENDPOINT = "registrations.json";
	
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
}
