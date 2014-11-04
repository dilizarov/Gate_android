package com.unlock.gate.utils;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by davidilizarov on 11/3/14.
 * For use when the Response only has a Header and no associated JSON.
 */
public class HeaderResponseRequest extends Request<Integer> {

    private final Response.Listener<Integer> mListener;
    private String mBody;
    private String mContentType;
    private HashMap mCustomHeaders;

    public HeaderResponseRequest(int method, String url, JSONObject body, Response.Listener<Integer> listener,
                                 Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        mBody = body.toString();
        mListener = listener;
        mContentType = "application/json";

        if (method == Method.POST) {
            RetryPolicy policy = new DefaultRetryPolicy(5000, 0, 5);
            setRetryPolicy(policy);
        }
    }

    @Override
    protected Response parseNetworkResponse(NetworkResponse response) {
        return Response.success(response.statusCode, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(Integer statusCode) {
        mListener.onResponse(statusCode);
    }

    @Override
    public Map getHeaders() throws AuthFailureError {
        if (mCustomHeaders != null) {
            return mCustomHeaders;
        }

        return super.getHeaders();
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        return mBody.getBytes();
    }

    @Override
    public String getBodyContentType() {
        return mContentType;
    }

    public String getContentType() {
        return mContentType;
    }

    public void setContentType(String mContentType) {
        this.mContentType = mContentType;
    }
}
