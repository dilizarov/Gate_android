package com.unlock.gate.utils;

import android.content.Context;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.unlock.gate.models.Network;

import org.json.JSONObject;

public class APIRequestProxy {
	
	private final String BASE_URL = "http://infinite-river-7560.herokuapp.com/api/v1/";
	
	private final String SESSION_ENDPOINT = "sessions.json";
	private final String REGISTRATION_ENDPOINT = "registrations.json";
    private final String NETWORKS_ENDPOINT = "networks.json";
    private final String AGGREGATE_ENDPOINT = "aggregate.json";
	
	private RequestQueue mRequestQueue;
	
	APIRequestProxy(Context context) {
		mRequestQueue = Volley.newRequestQueue(context.getApplicationContext());
	}
	
	private String getAbsoluteUrl(String endpoint) {
		return BASE_URL + endpoint;
	}

    // Requires user_id and auth_token as params
    private String addAuthAsURLParams(String url, JSONObject params) {
        StringBuilder buildURLParams = new StringBuilder(url);
        buildURLParams.append("?")
                      .append("user_id=")
                      .append(params.optString("user_id"))
                      .append("&")
                      .append("auth_token=")
                      .append(params.optString("auth_token"));

        return buildURLParams.toString();
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

        url = addAuthAsURLParams(url, params);

        JsonObjectRequest request = new JsonObjectRequest(Method.GET, url, null, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void leaveNetwork(Network network, JSONObject params, Response.Listener<Integer> listener, Response.ErrorListener errorListener) {
        //TODO: Probably want to figure out a better way to make these resourcesful strings with consideration of getAbsoluteUrl method.
        //TODO: Not a big deal though

        StringBuilder buildUrl  = new StringBuilder(BASE_URL);
        buildUrl.append("networks")
                .append("/")
                .append(network.getId())
                .append("/")
                .append("leave.json");

        String url = addAuthAsURLParams(buildUrl.toString(), params);

        HeaderResponseRequest request = new HeaderResponseRequest(Method.DELETE, url, params, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void getNetworkPosts(JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        StringBuilder buildUrl = new StringBuilder(BASE_URL);
        buildUrl.append("networks")
                .append("/")
                .append(params.optString("network_id"))
                .append("/")
                .append("posts.json");

        String url = addAuthAsURLParams(buildUrl.toString(), params);

        JsonObjectRequest request = new JsonObjectRequest(Method.GET, url, null, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void getAggregate(JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        String url = getAbsoluteUrl(AGGREGATE_ENDPOINT);
        url = addAuthAsURLParams(url, params);
        StringBuilder buildUrl = new StringBuilder(url);
        if (params.optInt("page", -1) != -1) buildUrl.append("&page=")
                                                     .append(params.optInt("page"));

        if (params.opt("infinite_scroll_time_buffer") != null)
            buildUrl.append("&infinite-scroll-time-buffer=")
                    .append(params.opt("infinite_scroll_time_buffer"));

        url = buildUrl.toString();

        JsonObjectRequest request = new JsonObjectRequest(Method.GET, url, null, listener, errorListener);

        mRequestQueue.add(request);
    }
}
