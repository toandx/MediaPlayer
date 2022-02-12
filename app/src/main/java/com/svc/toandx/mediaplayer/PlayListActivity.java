package com.svc.toandx.mediaplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class PlayListActivity extends AppCompatActivity {
    private Intent msgIntent;
    private ArrayList<Music> playList;
    private ListView list;
    private int songID;
    private PlayListAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_list);
        playList = new ArrayList<Music>();
        list=(ListView) findViewById(R.id.list);
        adapter=new PlayListAdapter(this,playList,-1);
        adapter.setOnClickListener(new PlayListAdapter.OnClickListener() {
            @Override
            public void onInfoBlockClickListener(int position) {
                msgIntent = new Intent("com.svc.toandx.mediaplayer.PlayService");
                msgIntent.putExtra("action",PlayService.ACTION_PLAYSONGID);
                msgIntent.putExtra("pos",position);
                adapter.setSelectedItem(position);
                sendBroadcast(msgIntent);
            }
            @Override
            public void onDelBtnClickListener(int position) {
                msgIntent = new Intent("com.svc.toandx.mediaplayer.PlayService");
                msgIntent.putExtra("action",PlayService.ACTION_REMOVESONGID);
                msgIntent.putExtra("pos",position);
                sendBroadcast(msgIntent);
                Log.d("toandx","Del button "+Integer.toString(position)+" "+Integer.toString(playList.size()));
                playList.remove(position);
                adapter.removeSong(position);
            }
        });
        list.setAdapter(adapter);
    }
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle args = intent.getBundleExtra("playlist");
            songID=intent.getIntExtra("songID",0);
            playList = (ArrayList<Music>) args.getSerializable("playlist");
            adapter.setVal(playList,songID);
        }
    };
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter("com.svc.toandx.mediaplayer.PlayListActivity"));
        msgIntent=new Intent("com.svc.toandx.mediaplayer.PlayService");
        msgIntent.putExtra("action",PlayService.ACTION_GETPLAYLIST);
        sendBroadcast(msgIntent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right);
    }
}