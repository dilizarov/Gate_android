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

    static class ViewHolder {
        TextView commenterName;
        TextView commentBody;
        TextView commentTimestamp;
        TextView upCountComment;
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
        ViewHolder viewHolder;

        if (mInflater == null) mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.comment_item, null);

            viewHolder = new ViewHolder();

            viewHolder.commenterName      = (TextView) convertView.findViewById(R.id.commenterName);
            viewHolder.commentBody        = (TextView) convertView.findViewById(R.id.commentBody);
            viewHolder.commentTimestamp   = (TextView) convertView.findViewById(R.id.commentTimestamp);
            viewHolder.upCountComment     = (TextView) convertView.findViewById(R.id.upCountComment);
            viewHolder.smileyCountComment = (ImageView) convertView.findViewById(R.id.smileyCountComment);
            viewHolder.commentStats       = (LinearLayout) convertView.findViewById(R.id.commentStats);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Comment comment = comments.get(position);

        

        viewHolder.commenterName.setText(comment.getName());
        viewHolder.commentBody.setText(comment.getBody());
        viewHolder.commentTimestamp.setText(comment.getTimestamp());

        return convertView;
    }
}
