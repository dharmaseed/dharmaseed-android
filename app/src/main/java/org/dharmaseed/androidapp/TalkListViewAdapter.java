package org.dharmaseed.androidapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by bbethke on 2/6/16.
 */
public class TalkListViewAdapter extends BaseAdapter {

    Context context;
    ArrayList<String> talkTitles;

    public TalkListViewAdapter(Context context, ArrayList<String> talkTitles) {
        this.context = context;
        this.talkTitles = talkTitles;
    }

    @Override
    public int getCount() {
        return talkTitles.size();
    }

    @Override
    public Object getItem(int position) {
        return talkTitles.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    class ViewHolder {
        private TextView talkViewTitle;
    }
    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder = null;
        if(view == null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.talk_list_view_item,null);
            viewHolder = new ViewHolder();
            viewHolder.talkViewTitle = (TextView) view.findViewById(R.id.talkViewTitle);
            view.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) view.getTag();
        }
        String title = talkTitles.get(position);
        viewHolder.talkViewTitle.setText(title);
        return view;
    }
}
