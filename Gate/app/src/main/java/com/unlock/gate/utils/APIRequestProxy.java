package com.unlock.gate.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.unlock.gate.R;
import com.unlock.gate.models.Comment;
import com.unlock.gate.models.Gate;
import com.unlock.gate.models.Post;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class APIRequestProxy {
	
	private final String BASE_URL = "https://infinite-river-7560.herokuapp.com/api/v1/";

    private final String ANDROID_API_KEY = "placeholder";

	private final String SESSION_ENDPOINT = "sessions.json";
	private final String REGISTRATION_ENDPOINT = "registrations.json";
    private final String GATES_ENDPOINT = "gates.json";
    private final String AGGREGATE_ENDPOINT = "aggregate.json";

    private final String FEED_TAG = "feed_requests";
	
	private RequestQueue mRequestQueue;

    private Context context;
    private SharedPreferences mSessionPreferences;

	APIRequestProxy(Context context) {
        this.context = context;
		mRequestQueue = Volley.newRequestQueue(context.getApplicationContext());
        mSessionPreferences = context.getSharedPreferences(
                context.getString(R.string.session_shared_preferences_key),
                Context.MODE_PRIVATE);
	}
	
	private String getAbsoluteUrl(String endpoint) {
		return BASE_URL + endpoint;
	}

    // Requires user_id and auth_token as params
//    private String addAuthAsURLParams(String url, JSONObject params) {
//        StringBuilder buildURLParams = new StringBuilder(url);
//        buildURLParams.append("?")
//                      .append("user_id=")
//                      .append(params.optString("user_id"))
//                      .append("&")
//                      .append("auth_token=")
//                      .append(params.optString("auth_token"));
//
//        return buildURLParams.toString();
//    }

    public String convertParamsToUrlParams(JSONObject params) {
        StringBuilder buildUrlParams = new StringBuilder();

        JSONArray keys = params.names();
        if (keys == null) return "";

        int len = keys.length();
        for (int i = 0; i < len; i++) {
            if (i == 0) buildUrlParams.append("?");
            else buildUrlParams.append("&");

            buildUrlParams.append(keys.optString(i))
                          .append("=")
                          .append(params.optString(keys.optString(i)));
        }

        return buildUrlParams.toString();
    }

    public JSONObject addAuthToParams(JSONObject params) {
        try {
            params.put("user_id",
                    mSessionPreferences.getString(context.getString(R.string.user_id_key), null));
            params.put("auth_token",
                    mSessionPreferences.getString(context.getString(R.string.user_auth_token_key), null));
        } catch (JSONException ex) {
            ex.printStackTrace();
        }

        return params;
    }
	
	public void login(JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
		JsonObjectRequest request = new JsonObjectRequest(Method.POST, getAbsoluteUrl(SESSION_ENDPOINT), params, listener, errorListener);
		
		mRequestQueue.add(request);
	}
	
	public void register(JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
		JsonObjectRequest request = new JsonObjectRequest(Method.POST, getAbsoluteUrl(REGISTRATION_ENDPOINT), params, listener, errorListener);
	
		mRequestQueue.add(request);
	}

    public void sendForgottonPasswordEmail(JSONObject params, Response.Listener<Integer> listener, Response.ErrorListener errorListener) {
        String url = "https://infinite-river-7560.herokuapp.com/forgot_password";

        url += convertParamsToUrlParams(params);

        HeaderResponseRequest request = new HeaderResponseRequest(Method.GET, url, params, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void getGates(JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        String url = getAbsoluteUrl(GATES_ENDPOINT);

        addAuthToParams(params);

        url += convertParamsToUrlParams(params);

        JsonObjectRequest request = new JsonObjectRequest(Method.GET, url, null, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void leaveGate(Gate gate, JSONObject params, Response.Listener<Integer> listener, Response.ErrorListener errorListener) {
        String url = BASE_URL + "gates/" + gate.getId() + "/leave.json";

        addAuthToParams(params);

        url += convertParamsToUrlParams(params);

        HeaderResponseRequest request = new HeaderResponseRequest(Method.DELETE, url, params, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void getGatePosts(Gate gate, JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        cancelAllFeedRequests();

        String url = BASE_URL + "gates/" + gate.getId() + "/posts.json";

        addAuthToParams(params);

        url += convertParamsToUrlParams(params);

        JsonObjectRequest request = new JsonObjectRequest(Method.GET, url, null, listener, errorListener);

        request.setTag(FEED_TAG);

        mRequestQueue.add(request);
    }

    public void getAggregate(JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        cancelAllFeedRequests();

        String url = getAbsoluteUrl(AGGREGATE_ENDPOINT);

        addAuthToParams(params);

        url += convertParamsToUrlParams(params);

        JsonObjectRequest request = new JsonObjectRequest(Method.GET, url, null, listener, errorListener);

        request.setTag(FEED_TAG);

        mRequestQueue.add(request);
    }

    public void getComments(Post post, JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        String url = BASE_URL + "posts/" + post.getId() + "/comments.json";

        addAuthToParams(params);

        url += convertParamsToUrlParams(params);

        JsonObjectRequest request = new JsonObjectRequest(Method.GET, url, null, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void createComment(Post post, JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        String url = BASE_URL + "posts/" + post.getId() + "/comments.json";

        addAuthToParams(params);

        JsonObjectRequest request = new JsonObjectRequest(Method.POST, url, params, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void createPost(Gate gate, JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        String url = BASE_URL + "gates/" + gate.getId() + "/posts.json";

        addAuthToParams(params);

        JsonObjectRequest request = new JsonObjectRequest(Method.POST, url, params, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void createGate(JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        JsonObjectRequest request = new JsonObjectRequest(Method.POST, getAbsoluteUrl(GATES_ENDPOINT), params, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void grantAccessToGates(String gatekeeperId, JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        String url = BASE_URL + "gatekeepers/" + gatekeeperId + "/grant_access.json";

        addAuthToParams(params);

        JsonObjectRequest request = new JsonObjectRequest(Method.POST, url, params, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void logout(JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        String url = BASE_URL + "sessions/logout.json";

        addAuthToParams(params);

        JsonObjectRequest request = new JsonObjectRequest(Method.POST, url, params, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void upPost(Post post, JSONObject params, Response.Listener<Integer> listener, Response.ErrorListener errorListener) {
        String url = BASE_URL + "posts/" + post.getId() + "/up.json";

        addAuthToParams(params);

        url += convertParamsToUrlParams(params);

        HeaderResponseRequest request = new HeaderResponseRequest(Method.GET, url, params, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void upComment(Comment comment, JSONObject params, Response.Listener<Integer> listener, Response.ErrorListener errorListener) {
        String url = BASE_URL + "comments/" + comment.getId() + "/up.json";

        addAuthToParams(params);

        url += convertParamsToUrlParams(params);

        HeaderResponseRequest request = new HeaderResponseRequest(Method.GET, url, params, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void cancelAllFeedRequests() {
        mRequestQueue.cancelAll(FEED_TAG);
    }

}
