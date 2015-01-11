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

import org.json.JSONObject;

public class APIRequestProxy {
	
	private final String BASE_URL = "https://infinite-river-7560.herokuapp.com/api/v1/";
	
	private final String SESSION_ENDPOINT = "sessions.json";
	private final String REGISTRATION_ENDPOINT = "registrations.json";
    private final String GATES_ENDPOINT = "gates.json";
    private final String AGGREGATE_ENDPOINT = "aggregate.json";

    private final String FEED_TAG = "feed_requests";
	
	private RequestQueue mRequestQueue;

    private Context context;
	
	APIRequestProxy(Context context) {
        this.context = context;
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

    public void sendForgottonPasswordEmail(JSONObject params, Response.Listener<Integer> listener, Response.ErrorListener errorListener) {
        String url = "https://infinite-river-7560.herokuapp.com/forgot_password?email=" + params.optString("email");

        HeaderResponseRequest request = new HeaderResponseRequest(Method.GET, url, params, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void getGates(JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        //Sadly, Volley does not offer a very fluid experience for get requests with params.
        String url = getAbsoluteUrl(GATES_ENDPOINT);

        url = addAuthAsURLParams(url, params);

        JsonObjectRequest request = new JsonObjectRequest(Method.GET, url, null, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void leaveGate(Gate gate, JSONObject params, Response.Listener<Integer> listener, Response.ErrorListener errorListener) {
        //TODO: Probably want to figure out a better way to make these resourcesful strings with consideration of getAbsoluteUrl method.
        //TODO: Not a big deal though

        StringBuilder buildUrl  = new StringBuilder(BASE_URL);
        buildUrl.append("gates")
                .append("/")
                .append(gate.getId())
                .append("/")
                .append("leave.json");

        String url = addAuthAsURLParams(buildUrl.toString(), params);

        HeaderResponseRequest request = new HeaderResponseRequest(Method.DELETE, url, params, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void getGatePosts(JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        StringBuilder buildUrl = new StringBuilder(BASE_URL);
        buildUrl.append("gates")
                .append("/")
                .append(params.optString("gate_id"))
                .append("/")
                .append("posts.json");

        String url = addAuthAsURLParams(buildUrl.toString(), params);

        StringBuilder addPageAndBuffer = new StringBuilder(url);

        if (params.optInt("page", -1) != -1) addPageAndBuffer.append("&page=")
                                                             .append(params.optInt("page"));

        if (params.opt("infinite_scroll_time_buffer") != null)
            addPageAndBuffer.append("&infinite-scroll-time-buffer=")
                            .append(params.opt("infinite_scroll_time_buffer"));

        url = addPageAndBuffer.toString();

        JsonObjectRequest request = new JsonObjectRequest(Method.GET, url, null, listener, errorListener);

        request.setTag(FEED_TAG);

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

        request.setTag(FEED_TAG);

        mRequestQueue.add(request);
    }

    public void getComments(JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        StringBuilder buildUrl = new StringBuilder(BASE_URL);
        buildUrl.append("posts")
                .append("/")
                .append(params.optString("post_id"))
                .append("/")
                .append("comments.json");

        String url = addAuthAsURLParams(buildUrl.toString(), params);

        JsonObjectRequest request = new JsonObjectRequest(Method.GET, url, null, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void createComment(Post post, JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        StringBuilder buildUrl = new StringBuilder(BASE_URL);
        buildUrl.append("posts")
                .append("/")
                .append(post.getId())
                .append("/")
                .append("comments.json");

        JsonObjectRequest request = new JsonObjectRequest(Method.POST, buildUrl.toString(), params, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void createPost(Gate gate, JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        StringBuilder buildUrl = new StringBuilder(BASE_URL);
        buildUrl.append("gates")
                .append("/")
                .append(gate.getId())
                .append("/")
                .append("posts.json");

        JsonObjectRequest request = new JsonObjectRequest(Method.POST, buildUrl.toString(), params, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void createGate(JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        JsonObjectRequest request = new JsonObjectRequest(Method.POST, getAbsoluteUrl(GATES_ENDPOINT), params, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void grantAccessToGates(String gatekeeperId, JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        StringBuilder buildUrl  = new StringBuilder(BASE_URL);
        buildUrl.append("gatekeepers")
                .append("/")
                .append(gatekeeperId)
                .append("/")
                .append("grant_access.json");

        JsonObjectRequest request = new JsonObjectRequest(Method.POST, buildUrl.toString(), params, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void logout(JSONObject params, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        //TODO: Probably want to figure out a better way to make these resourcesful strings with consideration of getAbsoluteUrl method.
        //TODO: Not a big deal though

        String url = BASE_URL + "sessions/logout.json";

        JsonObjectRequest request = new JsonObjectRequest(Method.POST, url, params, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void upPost(Post post, JSONObject params, Response.Listener<Integer> listener, Response.ErrorListener errorListener) {
        String url = BASE_URL + "posts/" + post.getId() + "/up.json?";

        SharedPreferences session = context.getSharedPreferences(context.getString(R.string.session_shared_preferences_key), Context.MODE_PRIVATE);

        if (params.optBoolean("revert")) url += "revert=true&";

        url += "user_id=" + session.getString(context.getString(R.string.user_id_key), null);
        url += "&auth_token=" + session.getString(context.getString(R.string.user_auth_token_key), null);

        HeaderResponseRequest request = new HeaderResponseRequest(Method.GET, url, params, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void upComment(Comment comment, JSONObject params, Response.Listener<Integer> listener, Response.ErrorListener errorListener) {
        String url = BASE_URL + "comments/" + comment.getId() + "/up.json?";

        SharedPreferences session = context.getSharedPreferences(context.getString(R.string.session_shared_preferences_key), Context.MODE_PRIVATE);

        if (params.optBoolean("revert")) url += "revert=true&";

        url += "user_id=" + session.getString(context.getString(R.string.user_id_key), null);
        url += "&auth_token=" + session.getString(context.getString(R.string.user_auth_token_key), null);

        HeaderResponseRequest request = new HeaderResponseRequest(Method.GET, url, params, listener, errorListener);

        mRequestQueue.add(request);
    }

    public void cancelAllFeedRequests() {
        mRequestQueue.cancelAll(FEED_TAG);
    }

}
