package com.unlock.gate.adapters;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

        if (position % 2 == 1) {
            convertView.setBackgroundColor(context.getResources().getColor(R.color.light_grey));
        } else {
            convertView.setBackgroundColor(context.getResources().getColor(R.color.white));
        }

        TextView name          = (TextView) convertView.findViewById(R.id.name);
        TextView body          = (TextView) convertView.findViewById(R.id.body);
        TextView timestamp     = (TextView) convertView.findViewById(R.id.timestamp);
        //TextView commentsCount = (TextView) convertView.findViewById(R.id.commentsCount);
        final ImageView smileyCount  = (ImageView) convertView.findViewById(R.id.smileyCount);
        final ImageView upPost       = (ImageView) convertView.findViewById(R.id.upPost);
        final TextView upCountPost   = (TextView) convertView.findViewById(R.id.upCountPost);

        final LinearLayout postStats = (LinearLayout) convertView.findViewById(R.id.postStats);

        final Post post = posts.get(position);

        name.setText(post.getName());
        body.setText(post.getBody());
        timestamp.setText(post.getTimestamp());
        /*commentsCount.setText(context.getResources()
                .getQuantityString(R.plurals.comments_count,
                                   post.getCommentCount(),
                                   post.getCommentCount()));*/


        upCountPost.setText(Integer.toString(post.getUpCount()));

        if (post.getUpCount() > 0) {
            upCountPost.setVisibility(View.VISIBLE);
            smileyCount.setVisibility(View.VISIBLE);
        } else {
            upCountPost.setVisibility(View.GONE);
            smileyCount.setVisibility(View.GONE);
        }

        if (post.getUped()) {
            upPost.setImageResource(R.drawable.ic_smiley);
        } else {
            upPost.setImageResource(R.drawable.ic_circle);
        }

        upPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleUpedViewState(post, upPost, upCountPost, smileyCount, postStats);

                try {
                    JSONObject params = new JSONObject();

                    if (!post.getUped()) params.put("revert", true);

                    Response.Listener<Integer> listener = new Response.Listener<Integer>() {
                        @Override
                        public void onResponse(Integer integer) {
                            //Don't do anything. Eagerly did actions assuming we request succeeds.
                        }
                    };

                    Response.ErrorListener errorListener = new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            VolleyErrorHandler volleyError = new VolleyErrorHandler(error);

                            Toast.makeText(context, volleyError.getMessage(), Toast.LENGTH_LONG).show();

                            handleUpedViewState(post, upPost, upCountPost, smileyCount, postStats);
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

    private void handleUpedViewState(Post post, ImageView upPost, TextView upCountPost, ImageView smileyCount, LinearLayout postStats) {
        if (post.getUped()) {
            post.setUped(false);
            post.setUpCount(post.getUpCount() - 1);
            upPost.setImageResource(R.drawable.ic_circle);

            if (post.getUpCount() == 0) {
                upCountPost.setText(Integer.toString(post.getUpCount()));
                upCountPost.setVisibility(View.GONE);
                smileyCount.setVisibility(View.GONE);

                int finalHeight = postStats.getHeight();

                ValueAnimator animator = slideAnimator(finalHeight, 0, postStats);
                animator.start();
            }
        } else {
            post.setUped(true);
            post.setUpCount(post.getUpCount() + 1);
            upPost.setImageResource(R.drawable.ic_smiley);
            upCountPost.setText(Integer.toString(post.getUpCount()));
            upCountPost.setVisibility(View.VISIBLE);
            smileyCount.setVisibility(View.VISIBLE);

            if (post.getUpCount() == 1) {
                final int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                final int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                postStats.measure(widthSpec, heightSpec);

                ValueAnimator animator = slideAnimator(0, postStats.getMeasuredHeight(), postStats);
                animator.start();
            }
        }
    }

    private ValueAnimator slideAnimator(int start, int end, final LinearLayout postStats) {

        ValueAnimator animator = ValueAnimator.ofInt(start, end);

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {

                int value = (Integer) animation.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = postStats.getLayoutParams();
                layoutParams.height = value;
                postStats.setLayoutParams(layoutParams);
            }
        });

        return animator;
    }


}
