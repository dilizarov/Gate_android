package com.unlock.gate.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.unlock.gate.R;
import com.unlock.gate.models.Network;
import com.unlock.gate.models.Post;
import com.unlock.gate.utils.APIRequestManager;
import com.unlock.gate.utils.VolleyErrorHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by davidilizarov on 10/22/14.
 */
public class FeedListAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater mInflater;
    private List<Post> posts;
    private Network network;
    private SharedPreferences mSessionPreferences;

    public FeedListAdapter(Context context, List<Post> posts, Network network) {
        this.context = context;
        this.posts = posts;
        this.network = network;
    }

    @Override
    public int getCount() {
        return posts.size();
    }

    @Override
    public Object getItem(int position) {
        return posts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (mInflater == null) mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) convertView = mInflater.inflate(R.layout.feed_item, null);

        TextView name          = (TextView) convertView.findViewById(R.id.name);
        TextView body          = (TextView) convertView.findViewById(R.id.body);
        TextView timestamp     = (TextView) convertView.findViewById(R.id.timestamp);
        TextView commentsCount = (TextView) convertView.findViewById(R.id.commentsCount);
        final TextView upPost  = (TextView) convertView.findViewById(R.id.upPost);
        final TextView upCountPost   = (TextView) convertView.findViewById(R.id.upCountPost);

        final Post post = posts.get(position);

        name.setText(post.getName());
        body.setText(post.getBody());
        timestamp.setText(post.getTimestamp());
        commentsCount.setText(context.getResources()
                .getQuantityString(R.plurals.comments_count,
                                   post.getCommentCount(),
                                   post.getCommentCount()));
        if (post.getUpCount() > 0) {
            upCountPost.setText(Integer.toString(post.getUpCount()));
            upPost.setTextColor(Color.BLUE);
        }

        upPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (post.getUped()) {
                    upPost.setTextColor(Color.BLACK);
                    post.setUped(false);
                    post.setUpCount(post.getUpCount() - 1);
                    if (post.getUpCount() == 0) upCountPost.setText("");
                } else {
                    upPost.setTextColor(Color.BLUE);
                    post.setUped(true);
                    post.setUpCount(post.getUpCount() + 1);
                    upCountPost.setText(Integer.toString(post.getUpCount()));
                }

                try {
                    JSONObject params = new JSONObject();

                    if (!post.getUped()) params.put("revert", true);

                    Response.Listener<Integer> listener = new Response.Listener<Integer>() {
                        @Override
                        public void onResponse(Integer integer) {

                        }
                    };

                    Response.ErrorListener errorListener = new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            VolleyErrorHandler volleyError = new VolleyErrorHandler(error);

                            Toast.makeText(context, volleyError.getMessage(), Toast.LENGTH_LONG).show();

                            if (post.getUped()) {
                                upPost.setTextColor(Color.BLACK);
                                post.setUped(false);
                                post.setUpCount(post.getUpCount() - 1);
                                if (post.getUpCount() == 0) upCountPost.setText("");
                            } else {
                                upPost.setTextColor(Color.BLUE);
                                post.setUped(true);
                                post.setUpCount(post.getUpCount() + 1);
                                upCountPost.setText(Integer.toString(post.getUpCount()));
                            }
                        }
                    };

                    APIRequestManager.getInstance().doRequest().upPost(post, params, listener, errorListener);
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }

            }
        });

        if (network == null) {
            TextView networkName = (TextView) convertView.findViewById(R.id.networkName);
            networkName.setText(post.getNetworkName());
        }

        return convertView;
    }
}
