package com.svc.toandx.mediaplayer;

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;

public class PlayListAdapter extends ArrayAdapter {
    private final Activity context;
    private ArrayList<Music> playList;
    private int selectedItem;
    public interface OnClickListener {
        public void onInfoBlockClickListener(int position);
        public void onDelBtnClickListener(int position);
    }
    private PlayListAdapter.OnClickListener onClickListener;
    public void setOnClickListener(PlayListAdapter.OnClickListener mOnClickListener)
    {
        this.onClickListener=mOnClickListener;
    }
    public PlayListAdapter(Activity context,ArrayList<Music> mPlayList,int mSelectedItem)
    {
        super(context,R.layout.activity_listview,mPlayList);
        this.context=context;
        this.playList = mPlayList;
        this.selectedItem=mSelectedItem;
        this.onClickListener=null;
    }
    public void setSelectedItem(int mSelectedItem)
    {
        selectedItem=mSelectedItem;
        this.notifyDataSetChanged();
    }
    public void setVal(ArrayList<Music> mPlayList,int mSelectedItem)
    {
        selectedItem=mSelectedItem;
        playList.clear();
        playList.addAll(mPlayList);
        this.notifyDataSetChanged();
    }
    public void removeSong(int pos)
    {
        playList.remove(pos);
        this.notifyDataSetChanged();
    }
    public View getView(final int position, View view, ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.playlistview, null,true);
        View blockInfo=(View) rowView.findViewById(R.id.block_info);
        ImageButton delBtn = (ImageButton) rowView.findViewById(R.id.del_btn);
        //this code gets references to objects in the listview_row.xml file
        TextView nameTextField = (TextView) rowView.findViewById(R.id.nameTextViewID);

        //this code sets the values of the objects to values from the arrays
        nameTextField.setText(playList.get(position).title);
        if (position == selectedItem)
        {
            nameTextField.setTextColor(Color.BLUE);
        }
        blockInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onClickListener!=null)
                    onClickListener.onInfoBlockClickListener(position);
            }
        });
        delBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onClickListener!=null)
                    onClickListener.onDelBtnClickListener(position);
            }
        });
        return rowView;

    };
}
