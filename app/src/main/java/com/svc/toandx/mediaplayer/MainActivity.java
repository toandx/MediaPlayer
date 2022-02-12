package com.svc.toandx.mediaplayer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.loader.content.CursorLoader;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private Handler myHandler = new Handler();;
    private int currentTime,finalTime;
    private SeekBar seekBar1;
    private ListView list;
    private Context mContext;
    private ArrayList<String> displayName;
    private ArrayList<String> path;
    private ArrayList<String> artist;
    private Boolean isPlaying,isServiceReady;
    private ImageButton pauseBtn;
    private TextView songNameTv,finalTimeTv,currentTimeTv;
    private EditText searchSongEt;
    private CustomListAdapter adapter;
    private int songId, sizeListSong;
    private ContentResolver cr;
    private String selection,sortOrder;
    private Uri uri;
    private Cursor cur;
    private Intent msgIntent;
    private String int2Time(int miliseconds)
    {
        return(Integer.toString(miliseconds/60000)+":"+Integer.toString((miliseconds%60000)/1000));
    }
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int action = intent.getIntExtra("action",0);
            if (action==PlayService.ACTION_NEXT)
            {
                playSong((songId+1+sizeListSong)%sizeListSong);
            } else if (action==PlayService.ACTION_PREV) {
                playSong((songId-1+sizeListSong)%sizeListSong);
            } else if (action==PlayService.ACTION_EXIT) {
                isPlaying=false;
                pauseBtn.setImageResource(R.drawable.ic_play);
                isServiceReady=false;
            } else if (action==PlayService.ACTION_PROGRESS) {
                seekBar1.setProgress(intent.getIntExtra("progress",0));
                currentTime=intent.getIntExtra("progress",0);
                currentTimeTv.setText(int2Time(currentTime));
            } else if (action==PlayService.ACTION_STATUS) {
                isPlaying=intent.getBooleanExtra("status",false);
                if (isPlaying) pauseBtn.setImageResource(R.drawable.ic_pause); else
                    pauseBtn.setImageResource(R.drawable.ic_play);
                songNameTv.setText(intent.getStringExtra("title"));
                finalTime = intent.getIntExtra("duration",0);
                currentTime=intent.getIntExtra("progress",0);
                finalTimeTv.setText(int2Time(finalTime));
                currentTimeTv.setText(int2Time(currentTime));
                seekBar1.setMax(finalTime);
                seekBar1.setProgress(currentTime);
            }

        }
    };
    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mediaPlayer= MediaPlayer.create(this,R.raw.song);
        isPlaying=false;
        seekBar1 = (SeekBar) findViewById(R.id.seekBar1);
        searchSongEt = (EditText) findViewById(R.id.et_search_song);
        pauseBtn = (ImageButton) findViewById(R.id.btn_pause);
        songNameTv = (TextView) findViewById(R.id.tv_songname);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        finalTimeTv = (TextView) findViewById(R.id.tv_finaltime);
        currentTimeTv = (TextView) findViewById(R.id.tv_currenttime);
        mContext=MainActivity.this;
        isServiceReady=false;
        seekBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress_save;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progress_save=progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                msgIntent = new Intent("com.svc.toandx.mediaplayer.PlayService");
                msgIntent.putExtra("action",PlayService.ACTION_SEEK);
                msgIntent.putExtra("seek",progress_save);
                sendBroadcast(msgIntent);
            }
        });
        list=(ListView) findViewById(R.id.list);
        /*list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playSong(position);
            }
        });*/
        displayName=new ArrayList<String>();
        path=new ArrayList<String>();
        artist=new ArrayList<String>();
        cr = getContentResolver();

        uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Dexter.withContext(mContext).withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        cur = cr.query(uri, null, selection, null, sortOrder);
                        int count = 0;
                        if(cur != null)
                        {
                            count = cur.getCount();

                            if(count > 0)
                            {
                                while(cur.moveToNext())
                                {
                                    displayName.add(cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.TITLE)));
                                    path.add(cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA)));
                                    artist.add(cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
                                    // Save to your list here
                                }

                            }
                        }

                        cur.close();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {

                    }
                }).check();
        Dexter.withContext(mContext).withPermission(Manifest.permission.READ_PHONE_STATE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {

                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        Toast.makeText(getApplicationContext(),"This app required read phone state permission to auto stop when a call comming",Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {

                    }
                }).check();
        songId=0;
        sizeListSong=path.size();
        adapter=new CustomListAdapter(this,displayName,artist,songId);
        adapter.setOnClickListener(new CustomListAdapter.OnClickListener() {
            @Override
            public void onInfoBlockClickListener(int position) {
                playSong(position);
            }

            @Override
            public void onAddBtnClickListener(int position) {
                msgIntent = new Intent("com.svc.toandx.mediaplayer.PlayService");
                msgIntent.putExtra("action",PlayService.ACTION_ADD2PLAYLIST);
                msgIntent.putExtra("title",displayName.get(position));
                msgIntent.putExtra("path",path.get(position));
                sendBroadcast(msgIntent);
                Toast.makeText(getApplicationContext(),"Add song to playlist",Toast.LENGTH_SHORT).show();
            }
        });
        list.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter("com.svc.toandx.mediaplayer.MainActivity"));
        msgIntent=new Intent("com.svc.toandx.mediaplayer.PlayService");
        msgIntent.putExtra("action",PlayService.ACTION_STATUS);
        sendBroadcast(msgIntent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    public void playSong(int mSongId)
    {
        if (!isServiceReady)
        {
            Intent serviceIntent = new Intent(this, PlayService.class);
            serviceIntent.putExtra("path",path.get(mSongId));
            serviceIntent.putExtra("title",displayName.get(mSongId));
            ContextCompat.startForegroundService(this,serviceIntent);
            isServiceReady=true;
        } else
        {
            Intent intent=new Intent("com.svc.toandx.mediaplayer.PlayService");
            intent.putExtra("action",PlayService.ACTION_START);
            intent.putExtra("path",path.get(mSongId));
            intent.putExtra("title",displayName.get(mSongId));
            intent.putExtra("position",mSongId);
            sendBroadcast(intent);
        }
        songId=mSongId;
        adapter.setSelectedItem(songId);
        isPlaying=true;
        songNameTv.setText(displayName.get(mSongId));
        pauseBtn.setImageResource(R.drawable.ic_pause);
    }
    public void searchSong(View view) {
        String name = searchSongEt.getText().toString();
        cr = getContentResolver();
        uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0 AND ("+MediaStore.Audio.Media.TITLE+ " LIKE '%"+name+"%' OR "+MediaStore.Audio.Media.ARTIST+" LIKE '%"+name+"%')";
        sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        cur = cr.query(uri, null, selection, null, sortOrder);
        int count = 0;
        displayName.clear();
        path.clear();
        artist.clear();
        if(cur != null)
        {
            count = cur.getCount();

            if(count > 0)
            {
                while(cur.moveToNext())
                {
                    displayName.add(cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.TITLE)));
                    path.add(cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA)));
                    artist.add(cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
                    // Save to your list here
                }

            }
        }
        songId=0;
        sizeListSong=path.size();
        cur.close();
        adapter.notifyDataSetChanged();
    }
    public void getPlayListActivity(View view)
    {
        Intent intent= new Intent(this, PlayListActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
    }
    public void startService(View view)
    {
        Intent serviceIntent = new Intent(this, PlayService.class);
        ContextCompat.startForegroundService(this,serviceIntent);
    }
    public void stopService(View view)
    {
        Intent serviceIntent = new Intent(this, PlayService.class);
        stopService(serviceIntent);
    }

    public void prevMedia(View view)
    {
        Intent intent=new Intent("com.svc.toandx.mediaplayer.PlayService");
        intent.putExtra("action",PlayService.ACTION_PREV);
        sendBroadcast(intent);
    }
    public void pauseMedia(View view)
    {
        Intent intent=new Intent("com.svc.toandx.mediaplayer.PlayService");
        intent.putExtra("action",PlayService.ACTION_PAUSE);
        sendBroadcast(intent);
    }
    public void nextMedia(View view)
    {
        Intent intent=new Intent("com.svc.toandx.mediaplayer.PlayService");
        intent.putExtra("action",PlayService.ACTION_NEXT);
        sendBroadcast(intent);
    }
    public void upVolume(View view)
    {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
    }
    public void downVolume(View view)
    {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
    }
    public void openFile(View view)
    {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 10);
    }
    private String getRealPathFromURI(Context context, Uri contentUri) {
        String[] projection = { MediaStore.Audio.Media.DATA };
        CursorLoader loader = new CursorLoader(context, contentUri, projection, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 10) {
            Uri uriSound = data.getData();
            //mediaPlayer=new MediaPlayer();
            //Intent intent=new Intent(getBaseContext(),PlayService.class);
            //intent.putExtra("path",uriSound.toString());
            //startService(intent);
            try {

                mediaPlayer.reset();
                //mediaPlayer.setDataSource(this, Uri.parse(getRealPathFromURI(this,uriSound)));
                mediaPlayer.setDataSource(this, uriSound);
                mediaPlayer.prepare();
                mediaPlayer.start();
                finalTime = mediaPlayer.getDuration();
                seekBar1.setMax(finalTime);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //Log.d("Sound",getRealPathFromURI(this,uriSound));

        }
    }
    /*private Runnable UpdateSongTime = new Runnable() {
        public void run() {
            startTime = mediaPlayer.getCurrentPosition();
            Log.d("Log",Integer.toString(startTime));
            seekBar1.setProgress((int)startTime);
            myHandler.postDelayed(this,100);
        }
    };*/

}
