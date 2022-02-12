package com.svc.toandx.mediaplayer;

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class CustomListAdapter extends ArrayAdapter {
    private final Activity context;
    private ArrayList<String> name;
    private ArrayList<String> info;
    private int selectedItem;
    public interface OnClickListener {
        public void onInfoBlockClickListener(int position);
        public void onAddBtnClickListener(int position);
    }
    private OnClickListener onClickListener;
    public void setOnClickListener(OnClickListener mOnClickListener)
    {
        this.onClickListener=mOnClickListener;
    }
    public CustomListAdapter(Activity context,ArrayList<String> name, ArrayList<String> info,int mSelectedItem)
    {
        super(context,R.layout.activity_listview,name);
        this.context=context;
        this.name=name;
        this.info=info;
        this.selectedItem=mSelectedItem;
        this.onClickListener=null;
    }
    public void setSelectedItem(int mSelectedItem)
    {
        selectedItem=mSelectedItem;
        this.notifyDataSetChanged();
    }
    public View getView(final int position, View view, ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.activity_listview, null,true);
        View blockInfo=(View) rowView.findViewById(R.id.block_info);
        ImageButton addBtn = (ImageButton) rowView.findViewById(R.id.add_btn);
        //this code gets references to objects in the listview_row.xml file
        TextView nameTextField = (TextView) rowView.findViewById(R.id.nameTextViewID);
        TextView infoTextField = (TextView) rowView.findViewById(R.id.infoTextViewID);

        //this code sets the values of the objects to values from the arrays
        nameTextField.setText(name.get(position));
        infoTextField.setText(info.get(position));
        if (position == selectedItem)
        {
            nameTextField.setTextColor(Color.BLUE);
            infoTextField.setTextColor(Color.BLUE);
        }
        blockInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onClickListener!=null)
                    onClickListener.onInfoBlockClickListener(position);
            }
        });
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onClickListener!=null)
                    onClickListener.onAddBtnClickListener(position);
            }
        });

        return rowView;

    };
}
/*

*/

