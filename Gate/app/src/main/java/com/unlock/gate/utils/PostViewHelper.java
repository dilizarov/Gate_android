package com.unlock.gate.utils;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.unlock.gate.R;
import com.unlock.gate.models.Post;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by davidilizarov on 12/15/14.
 */
public class PostViewHelper {

    public static void handleUpBehavior(final Context context, final Post post, final ImageView upPost, final TextView upCountPost, final ImageView smileyCount, final LinearLayout postStats) {
        upCountPost.setText(Integer.toString(post.getUpCount()));

        if (post.getUpCount() > 0) {
            upCountPost.setVisibility(View.VISIBLE);
            smileyCount.setVisibility(View.VISIBLE);

            measurePostStats(postStats);
            setPostStatsHeight(postStats, postStats.getMeasuredHeight());
        } else {
            upCountPost.setVisibility(View.GONE);
            smileyCount.setVisibility(View.GONE);

            if (post.getCommentCount() == 0) {
                setPostStatsHeight(postStats, 0);
            }
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
                            //Don't do anything. Eagerly did actions assuming the request succeeds.
                        }
                    };

                    Response.ErrorListener errorListener = new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            VolleyErrorHandler volleyError = new VolleyErrorHandler(error);

                            Butter.down(context, volleyError.getMessage());

                            handleUpedViewState(post, upPost, upCountPost, smileyCount, postStats);
                        }
                    };

                    APIRequestManager.getInstance().doRequest().upPost(post, params, listener, errorListener);
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }

            }
        });
    }

    public static void handleCommentBehavior(final Context context, final Post post, TextView commentsCount, ImageView commentsCountBubble, LinearLayout postStats) {
        commentsCount.setText(Integer.toString(post.getCommentCount()));

        if (post.getCommentCount() > 0) {
            commentsCount.setVisibility(View.VISIBLE);
            commentsCountBubble.setVisibility(View.VISIBLE);

            measurePostStats(postStats);
            setPostStatsHeight(postStats, postStats.getMeasuredHeight());
        } else {
            commentsCount.setVisibility(View.GONE);
            commentsCountBubble.setVisibility(View.GONE);
        }
    }

    public static void handleUpedViewState(Post post, ImageView upPost, TextView upCountPost, ImageView smileyCount, LinearLayout postStats) {
        if (post.getUped()) {
            post.setUped(false);
            post.setUpCount(post.getUpCount() - 1);
            upPost.setImageResource(R.drawable.ic_circle);

            if (post.getUpCount() == 0) {
                upCountPost.setVisibility(View.GONE);
                smileyCount.setVisibility(View.GONE);

                if (post.getCommentCount() == 0) {
                    collapsePostStats(postStats);
                }
            }

            upCountPost.setText(Integer.toString(post.getUpCount()));
        } else {
            post.setUped(true);
            post.setUpCount(post.getUpCount() + 1);
            upPost.setImageResource(R.drawable.ic_smiley);
            upCountPost.setText(Integer.toString(post.getUpCount()));
            upCountPost.setVisibility(View.VISIBLE);
            smileyCount.setVisibility(View.VISIBLE);

            if (post.getUpCount() == 1 && post.getCommentCount() == 0) {
                expandPostStats(postStats);
            }
        }
    }

    public static void expandPostStats(LinearLayout postStats) {
        measurePostStats(postStats);

        ValueAnimator animator = slideAnimator(0, postStats.getMeasuredHeight(), postStats);
        animator.start();
    }

    public static void collapsePostStats(LinearLayout postStats) {
        int finalHeight = postStats.getHeight();

        ValueAnimator animator = slideAnimator(finalHeight, 0, postStats);
        animator.start();
    }

    public static ValueAnimator slideAnimator(int start, int end, final LinearLayout postStats) {

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

    private static void measurePostStats(LinearLayout postStats) {
        final int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        final int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        postStats.measure(widthSpec, heightSpec);
    }

    private static void setPostStatsHeight(LinearLayout postStats, int height) {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) postStats.getLayoutParams();
        layoutParams.height = height;
        postStats.setLayoutParams(layoutParams);
    }

}
