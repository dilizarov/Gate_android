package com.unlock.gate.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.unlock.gate.R;
import com.unlock.gate.models.Comment;
import com.unlock.gate.utils.APIRequestManager;
import com.unlock.gate.utils.Butter;
import com.unlock.gate.utils.VolleyErrorHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by davidilizarov on 11/10/14.
 */
public class CommentsListAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater mInflater;
    private List<Comment> comments;

    static class ViewHolder {
        TextView commenterName;
        TextView commentBody;
        TextView commentTimestamp;
        TextView upCountComment;
        ImageView upComment;
        ImageView smileyCountComment;
        LinearLayout commentStats;
    }

    public CommentsListAdapter(Context context, List<Comment> comments) {
        this.context = context;
        this.comments = comments;
    }

    @Override
    public int getCount() {
        return comments.size();
    }

    @Override
    public Object getItem(int position) {
        return comments.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;

        if (mInflater == null) mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.comment_item, null);

            viewHolder = new ViewHolder();

            viewHolder.commenterName      = (TextView) convertView.findViewById(R.id.commenterName);
            viewHolder.commentBody        = (TextView) convertView.findViewById(R.id.commentBody);
            viewHolder.commentTimestamp   = (TextView) convertView.findViewById(R.id.commentTimestamp);
            viewHolder.upCountComment     = (TextView) convertView.findViewById(R.id.upCountComment);
            viewHolder.upComment          = (ImageView) convertView.findViewById(R.id.upComment);
            viewHolder.smileyCountComment = (ImageView) convertView.findViewById(R.id.smileyCountComment);
            viewHolder.commentStats       = (LinearLayout) convertView.findViewById(R.id.commentStats);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if (position % 2 == 0) {
            convertView.setBackgroundColor(context.getResources().getColor(R.color.light_grey));
        } else {
            convertView.setBackgroundColor(context.getResources().getColor(R.color.white));
        }


        final Comment comment = comments.get(position);

        viewHolder.commenterName.setText(comment.getName());
        viewHolder.commentBody.setText(comment.getBody());
        viewHolder.commentTimestamp.setText(comment.getTimestamp());

        viewHolder.upComment.setImageResource(comment.getUped()
                                              ? R.drawable.ic_small_thumb_up
                                              : R.drawable.ic_small_greyed_out_thumb_up);

        viewHolder.upCountComment.setText(Integer.toString(comment.getUpCount()));
        if (comment.getUpCount() > 0) {
            viewHolder.upCountComment.setVisibility(View.VISIBLE);
            viewHolder.smileyCountComment.setVisibility(View.VISIBLE);
        } else {
            viewHolder.upCountComment.setVisibility(View.INVISIBLE);
            viewHolder.smileyCountComment.setVisibility(View.INVISIBLE);
        }

        viewHolder.upComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                toggleUpComment(comment, viewHolder);

                try {
                    JSONObject params = new JSONObject();

                    if (!comment.getUped()) params.put("revert", true);

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

                            toggleUpComment(comment, viewHolder);
                        }
                    };

                    APIRequestManager.getInstance().doRequest().upComment(comment, params, listener, errorListener);
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            }
        });

        return convertView;
    }

    private void toggleUpComment(Comment comment, ViewHolder viewHolder) {
        if (comment.getUped()) {
            comment.setUped(false);
            comment.setUpCount(comment.getUpCount() - 1);
            viewHolder.upComment.setImageResource(R.drawable.ic_small_greyed_out_thumb_up);
        } else {
            comment.setUped(true);
            comment.setUpCount(comment.getUpCount() + 1);
            viewHolder.upComment.setImageResource(R.drawable.ic_small_thumb_up);
        }

        viewHolder.upCountComment.setText(Integer.toString(comment.getUpCount()));
        if (comment.getUpCount() > 0) {
            viewHolder.upCountComment.setVisibility(View.VISIBLE);
            viewHolder.smileyCountComment.setVisibility(View.VISIBLE);
        } else {
            viewHolder.upCountComment.setVisibility(View.INVISIBLE);
            viewHolder.smileyCountComment.setVisibility(View.INVISIBLE);
        }
    }
}
