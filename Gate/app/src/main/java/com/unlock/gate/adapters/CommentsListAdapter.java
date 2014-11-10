package com.unlock.gate.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.unlock.gate.R;
import com.unlock.gate.models.Comment;

import java.util.List;

/**
 * Created by davidilizarov on 11/10/14.
 */
public class CommentsListAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater mInflater;
    private List<Comment> comments;

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
        if (mInflater == null) mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) convertView = mInflater.inflate(R.layout.comment_item, null);

        TextView commenterName    = (TextView) convertView.findViewById(R.id.commenterName);
        TextView commentBody      = (TextView) convertView.findViewById(R.id.commentBody);
        TextView commentTimestamp = (TextView) convertView.findViewById(R.id.commentTimestamp);

        Comment comment = comments.get(position);

        commenterName.setText(comment.getName());
        commentBody.setText(comment.getBody());
        commentTimestamp.setText(comment.getTimestamp());

        return convertView;
    }
}
