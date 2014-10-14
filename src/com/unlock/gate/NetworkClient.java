package com.unlock.gate;

import com.loopj.android.http.*;

public class NetworkClient {
	private static final String BASE_URL = "http://infinite-river-7560.herokuapp.com";
	
	private static AsyncHttpClient client = new AsyncHttpClient();
	
	public static void get(String uri, RequestParams params, AsyncHttpResponseHandler responseHandler) {
		client.get(getAbsoluteUrl(uri), params, responseHandler);
	}
	
	public static void post(String uri, RequestParams params, AsyncHttpResponseHandler responseHandler) {
		client.post(uri, params, responseHandler);
	}
	
	private static String getAbsoluteUrl(String relativeUrl) {
		return BASE_URL + relativeUrl;
	}
}
