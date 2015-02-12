package com.unlock.gate.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.volley.DefaultRetryPolicy;
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

    // Not secure right now, but in time we'll deal with that
    private final String API_KEY = "09b19f4a-6e4d-475a-b7c8-a369c60e9f83";

	private final String SESSION_ENDPOINT = "sessions.json";
	private final String REGISTRATION_ENDPOINT = "registrations.json";
    private final String GATES_ENDPOINT = "gates.json";
    private final String AGGREGATE_ENDPOINT = "aggregate.json";
    private final String KEY_ENDPOINT = "keys.json";

    private final String FEED_TAG = "feed_requests";
	
	private RequestQueue mRequestQueue;

    private Context context;
    private SharedPreferences mSessionPreferences;

    // No retries!
    final private DefaultRetryPolicy retryPolicy = new DefaultRetryPolicy(0, 0, 0);

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

    public JSONObject addAPIKeyToParams(JSONObject params) {
        try {
            params.put("api_key", API_KEY);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }

        return params;
    }

    public void login(JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
		addAPIKeyToParams(params);
        JsonObjectRequest request = new JsonObjectRequest(Method.POST, getAbsoluteUrl(SESSION_ENDPOINT), params, listener, errorListener);

        request.setRetryPolicy(retryPolicy);

		mRequestQueue.add(request);
	}
	
	public void register(JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        addAPIKeyToParams(params);
        JsonObjectRequest request = new JsonObjectRequest(Method.POST, getAbsoluteUrl(REGISTRATION_ENDPOINT), params, listener, errorListener);

        request.setRetryPolicy(retryPolicy);

        mRequestQueue.add(request);
	}

    public void sendForgottonPasswordEmail(JSONObject params, Response.Listener<Integer> listener, Response.ErrorListener errorListener) {
        String url = "https://infinite-river-7560.herokuapp.com/forgot_password";

        url += convertParamsToUrlParams(params);

        HeaderResponseRequest request = new HeaderResponseRequest(Method.GET, url, params, listener, errorListener);

        request.setRetryPolicy(retryPolicy);

        mRequestQueue.add(request);
    }

    public void getGates(JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        String url = getAbsoluteUrl(GATES_ENDPOINT);

        addAPIKeyToParams(params);
        addAuthToParams(params);

        url += convertParamsToUrlParams(params);

        JsonObjectRequest request = new JsonObjectRequest(Method.GET, url, null, listener, errorListener);

        request.setRetryPolicy(retryPolicy);

        mRequestQueue.add(request);
    }

    public void leaveGate(Gate gate, JSONObject params, Response.Listener<Integer> listener, Response.ErrorListener errorListener) {
        String url = BASE_URL + "gates/" + gate.getId() + "/leave.json";

        addAPIKeyToParams(params);
        addAuthToParams(params);

        url += convertParamsToUrlParams(params);

        HeaderResponseRequest request = new HeaderResponseRequest(Method.DELETE, url, params, listener, errorListener);

        request.setRetryPolicy(retryPolicy);

        mRequestQueue.add(request);
    }

    public void getGatePosts(Gate gate, JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        String url = BASE_URL + "gates/" + gate.getId() + "/posts.json";

        addAPIKeyToParams(params);
        addAuthToParams(params);

        url += convertParamsToUrlParams(params);

        JsonObjectRequest request = new JsonObjectRequest(Method.GET, url, null, listener, errorListener);

        request.setTag(FEED_TAG);

        request.setRetryPolicy(retryPolicy);

        mRequestQueue.add(request);
    }

    public void getAggregate(JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        String url = getAbsoluteUrl(AGGREGATE_ENDPOINT);

        addAPIKeyToParams(params);
        addAuthToParams(params);

        url += convertParamsToUrlParams(params);

        JsonObjectRequest request = new JsonObjectRequest(Method.GET, url, null, listener, errorListener);

        request.setTag(FEED_TAG);

        request.setRetryPolicy(retryPolicy);

        mRequestQueue.add(request);
    }

    public void getComments(Post post, JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        String url = BASE_URL + "posts/" + post.getId() + "/comments.json";

        addAPIKeyToParams(params);
        addAuthToParams(params);

        url += convertParamsToUrlParams(params);

        JsonObjectRequest request = new JsonObjectRequest(Method.GET, url, null, listener, errorListener);

        request.setRetryPolicy(retryPolicy);

        mRequestQueue.add(request);
    }

    public void createComment(Post post, JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        String url = BASE_URL + "posts/" + post.getId() + "/comments.json";

        addAPIKeyToParams(params);
        addAuthToParams(params);

        JsonObjectRequest request = new JsonObjectRequest(Method.POST, url, params, listener, errorListener);

        request.setRetryPolicy(retryPolicy);

        mRequestQueue.add(request);
    }

    public void createPost(Gate gate, JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        String url = BASE_URL + "gates/" + gate.getId() + "/posts.json";

        addAPIKeyToParams(params);
        addAuthToParams(params);

        JsonObjectRequest request = new JsonObjectRequest(Method.POST, url, params, listener, errorListener);

        request.setRetryPolicy(retryPolicy);

        mRequestQueue.add(request);
    }

    public void createGate(JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        addAPIKeyToParams(params);
        addAuthToParams(params);

        JsonObjectRequest request = new JsonObjectRequest(Method.POST, getAbsoluteUrl(GATES_ENDPOINT), params, listener, errorListener);

        request.setRetryPolicy(retryPolicy);

        mRequestQueue.add(request);
    }

    public void grantAccessToGates(String gatekeeperId, JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        String url = BASE_URL + "gatekeepers/" + gatekeeperId + "/grant_access.json";

        addAPIKeyToParams(params);
        addAuthToParams(params);

        JsonObjectRequest request = new JsonObjectRequest(Method.POST, url, params, listener, errorListener);

        request.setRetryPolicy(retryPolicy);

        mRequestQueue.add(request);
    }

    public void logout(JSONObject params, Response.Listener<Integer> listener, Response.ErrorListener errorListener) {
        String url = BASE_URL + "sessions/logout.json";

        addAPIKeyToParams(params);
        addAuthToParams(params);

        HeaderResponseRequest request = new HeaderResponseRequest(Method.POST, url, params, listener, errorListener);

        request.setRetryPolicy(retryPolicy);

        mRequestQueue.add(request);
    }

    public void upPost(Post post, JSONObject params, Response.Listener<Integer> listener, Response.ErrorListener errorListener) {
        String url = BASE_URL + "posts/" + post.getId() + "/up.json";

        addAPIKeyToParams(params);
        addAuthToParams(params);

        url += convertParamsToUrlParams(params);

        HeaderResponseRequest request = new HeaderResponseRequest(Method.GET, url, params, listener, errorListener);

        request.setRetryPolicy(retryPolicy);

        mRequestQueue.add(request);
    }


    public void upComment(Comment comment, JSONObject params, Response.Listener<Integer> listener, Response.ErrorListener errorListener) {
        String url = BASE_URL + "comments/" + comment.getId() + "/up.json";

        addAPIKeyToParams(params);
        addAuthToParams(params);

        url += convertParamsToUrlParams(params);

        HeaderResponseRequest request = new HeaderResponseRequest(Method.GET, url, params, listener, errorListener);

        request.setRetryPolicy(retryPolicy);

        mRequestQueue.add(request);
    }

    public void getKey(JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        addAPIKeyToParams(params);
        addAuthToParams(params);

        JsonObjectRequest request = new JsonObjectRequest(Method.POST, getAbsoluteUrl(KEY_ENDPOINT), params, listener, errorListener);

        request.setRetryPolicy(retryPolicy);

        mRequestQueue.add(request);
    }

    public void processKey(String key, JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        String url = BASE_URL + "keys/" + key + "/process.json";

        addAPIKeyToParams(params);
        addAuthToParams(params);

        JsonObjectRequest request = new JsonObjectRequest(Method.POST, url, params, listener, errorListener);

        request.setRetryPolicy(retryPolicy);

        mRequestQueue.add(request);
    }

    public void cancelAllFeedRequests() {
        mRequestQueue.cancelAll(FEED_TAG);
    }

}
