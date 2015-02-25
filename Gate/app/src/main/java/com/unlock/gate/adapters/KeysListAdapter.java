package com.unlock.gate.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.unlock.gate.R;
import com.unlock.gate.models.Key;

import java.util.List;

/**
 * Created by davidilizarov on 2/24/15.
 */
public class KeysListAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater mInflater;
    private List<Key> keys;

    static class ViewHolder {
        TextView key;
        TextView expiresSoon;
        TextView keyGates;
    }

    public KeysListAdapter(Context context, List<Key> keys) {
        this.context = context;
        this.keys = keys;
    }

    @Override
    public int getCount() {
        return keys.size();
    }

    @Override
    public Object getItem(int position) {
        return keys.get(position);
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
            convertView = mInflater.inflate(R.layout.key_item, null);

            viewHolder = new ViewHolder();

            viewHolder.key = (TextView) convertView.findViewById(R.id.key);
            viewHolder.expiresSoon = (TextView) convertView.findViewById(R.id.keyExpiresSoon);
            viewHolder.keyGates = (TextView) convertView.findViewById(R.id.keyGates);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if (position % 2 == 1) {
            convertView.setBackgroundColor(context.getResources().getColor(R.color.light_grey));
        } else {
            convertView.setBackgroundColor(context.getResources().getColor(R.color.white));
        }

        final Key key = keys.get(position);

        viewHolder.key.setText(key.getKey());
        if (key.expiresSoon()) viewHolder.expiresSoon.setText("Expires in " + key.expireTime());
        viewHolder.expiresSoon.setVisibility(key.expiresSoon()
                ? View.VISIBLE
                : View.INVISIBLE);
        viewHolder.keyGates.setText(key.gatesList());

        return convertView;
    }
}
