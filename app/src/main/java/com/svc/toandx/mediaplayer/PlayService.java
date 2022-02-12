package com.svc.toandx.mediaplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.session.MediaButtonReceiver;

import java.util.ArrayList;

import static android.app.Service.START_STICKY;
public class PlayService extends Service {
    private MediaPlayer mediaPlayer;
    private Music music;
    public static final String CHANNEL_ID = "MusicService";
    public static final int NOTIFY_ID = 1;
    public static final int ACTION_PAUSE = 2;
    public static final int ACTION_START = 3;
    public static final int ACTION_EXIT = 4;
    public static final int ACTION_PREV = 5;
    public static final int ACTION_NEXT = 6;
    public static final int ACTION_PROGRESS = 7;
    public static final int ACTION_STATUS = 8;
    public static final int ACTION_SEEK = 9;
    public static final int ACTION_ADD2PLAYLIST = 10;
    public static final int ACTION_GETPLAYLIST = 11;
    public static final int ACTION_PLAYSONGID = 12;
    public static final int ACTION_REMOVESONGID = 13;
    private NotificationManager notificationManager;
    private RemoteViews collapsedView;
    private Notification notification;
    private Handler handler;
    private Runnable runnable;
    private int songID;
    private String path,title;
    private Intent replyIntent;
    private ArrayList<Music> playList;
    private TelephonyManager telephonyManager;
    private MediaSessionCompat mediaSessionCompat;
    private void configMediaSession()
    {
        mediaSessionCompat = new MediaSessionCompat(getApplicationContext(),"MyMediaSession");
        mediaSessionCompat.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPause() {
                //Toast.makeText(getApplicationContext(),"on Pause",Toast.LENGTH_LONG).show();
                /*replyIntent = new Intent("com.svc.toandx.mediaplayer.PlayService");
                replyIntent.putExtra("action",ACTION_PAUSE);
                sendBroadcast(replyIntent);*/
                super.onPause();
            }

            @Override
            public void onPlay() {
                //Toast.makeText(getApplicationContext(),"on Play",Toast.LENGTH_LONG).show();
                super.onPlay();
            }
            @Override
            public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
                //Toast.makeText(getApplicationContext(),"onMediaButtonEvent Handle",Toast.LENGTH_LONG).show();
                KeyEvent ke = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (ke != null && ke.getAction() == KeyEvent.ACTION_DOWN) {
                    int keyCode = ke.getKeyCode();
                    if (keyCode==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    {
                        replyIntent = new Intent("com.svc.toandx.mediaplayer.PlayService");
                        replyIntent.putExtra("action",ACTION_PAUSE);
                        sendBroadcast(replyIntent);
                    }
                }
                return super.onMediaButtonEvent(mediaButtonIntent);
            }
        });
        PlaybackStateCompat state = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 0, 0)
                .build();
        mediaSessionCompat.setPlaybackState(state);
        mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSessionCompat.setActive(true);
    }
    private void startPlayService(Intent intent)
    {
        path=intent.getStringExtra("path");
        title=intent.getStringExtra("title");
        music=new Music(title,path);
        playList.clear();
        playList.add(music);
        songID=0;
        try
        {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(getApplicationContext(), Uri.parse(path));
            mediaPlayer.prepare();
            mediaPlayer.start();
            collapsedView.setTextViewText(R.id.tv_songname,title);
            collapsedView.setInt(R.id.btn_pause_noti,"setImageResource",R.drawable.ic_pause);
            sendStatus();
            handler.postDelayed(runnable,100);
            updateNotify();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    private PhoneStateListener phoneStateListener = new PhoneStateListener(){
        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
            super.onCallStateChanged(state, phoneNumber);
            if (state== TelephonyManager.CALL_STATE_RINGING)
            {
                mediaPlayer.pause();
                collapsedView.setInt(R.id.btn_pause_noti, "setImageResource", R.drawable.ic_play);
                handler.removeCallbacks(runnable);
                updateNotify();
                sendStatus();
                // Call is comming
            }
            if (state== TelephonyManager.CALL_STATE_OFFHOOK)
            {
                mediaPlayer.pause();
                collapsedView.setInt(R.id.btn_pause_noti, "setImageResource", R.drawable.ic_play);
                handler.removeCallbacks(runnable);
                updateNotify();
                sendStatus();
                // On a call
            }
            if (state== TelephonyManager.CALL_STATE_IDLE)
            {
                /*mediaPlayer.start();
                collapsedView.setInt(R.id.btn_pause_noti, "setImageResource", R.drawable.ic_pause);
                handler.postDelayed(runnable,100);
                updateNotify();
                sendStatus();*/
                // Netheir on call or a call coming
            }
        }
    };
    private BroadcastReceiver becomingNoisyReceive=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction()))
            {
                mediaPlayer.pause();
                collapsedView.setInt(R.id.btn_pause_noti, "setImageResource", R.drawable.ic_play);
                handler.removeCallbacks(runnable);
                updateNotify();
                sendStatus();
            }
        }
    };
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            int action=intent.getIntExtra("action",0);
            if (action==ACTION_START)
            {
                startPlayService(intent);
            } else if (action==ACTION_PAUSE) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    collapsedView.setInt(R.id.btn_pause_noti, "setImageResource", R.drawable.ic_play);
                    handler.removeCallbacks(runnable);
                    updateNotify();
                } else {
                    mediaPlayer.start();
                    collapsedView.setInt(R.id.btn_pause_noti, "setImageResource", R.drawable.ic_pause);
                    handler.postDelayed(runnable,100);
                    updateNotify();
                }
                sendStatus();
            } else if (action==ACTION_NEXT) {
                playSong((songID+1+playList.size())%playList.size());
            } else if (action==ACTION_PREV) {
                playSong((songID-1+playList.size())%playList.size());
            } else if (action==ACTION_EXIT) {
                stopSelf();
            } else if (action==ACTION_STATUS) {
                sendStatus();
            } else if (action==ACTION_SEEK)
            {
                int seek=intent.getIntExtra("seek",0);
                mediaPlayer.seekTo(seek);
            } else if (action==ACTION_ADD2PLAYLIST)
            {
                music=new Music(intent.getStringExtra("title"),intent.getStringExtra("path"));
                playList.add(music);
            } else if (action==ACTION_GETPLAYLIST)
            {
                replyIntent = new Intent("com.svc.toandx.mediaplayer.PlayListActivity");
                Bundle bundle=new Bundle();
                bundle.putSerializable("playlist",playList);
                replyIntent.putExtra("playlist",bundle);
                replyIntent.putExtra("songID",songID);
                sendBroadcast(replyIntent);
            } else if (action==ACTION_PLAYSONGID)
            {
                int pos=intent.getIntExtra("pos",0);
                playSong(pos);
            } else if (action==ACTION_REMOVESONGID)
            {
                int pos=intent.getIntExtra("pos",0);
                playList.remove(pos);
            }
        }
    };
    private void playSong(int mSongID)
    {
        try
        {
            songID=mSongID;
            mediaPlayer.reset();
            mediaPlayer.setDataSource(getApplicationContext(), Uri.parse(playList.get(songID).path));
            mediaPlayer.prepare();
            mediaPlayer.start();
            collapsedView.setTextViewText(R.id.tv_songname,playList.get(songID).title);
            collapsedView.setInt(R.id.btn_pause_noti,"setImageResource",R.drawable.ic_pause);
            sendStatus();
            handler.postDelayed(runnable,100);
            updateNotify();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    private void sendStatus()
    {
        replyIntent = new Intent("com.svc.toandx.mediaplayer.MainActivity");
        replyIntent.putExtra("action",ACTION_STATUS);
        replyIntent.putExtra("title",playList.get(songID).title);
        replyIntent.putExtra("status",mediaPlayer.isPlaying());
        replyIntent.putExtra("duration",mediaPlayer.getDuration());
        replyIntent.putExtra("progress",mediaPlayer.getCurrentPosition());
        sendBroadcast(replyIntent);
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onCreate()
    {
        super.onCreate();
        configMediaSession();
        mediaPlayer= MediaPlayer.create(this,R.raw.song);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        playList= new ArrayList<Music>();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                playSong((songID+1+playList.size())%playList.size());
            }
        });
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mediaSessionCompat,intent);
        registerReceiver(receiver, new IntentFilter("com.svc.toandx.mediaplayer.PlayService"));
        registerReceiver(becomingNoisyReceive,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        collapsedView = new RemoteViews(getPackageName(), R.layout.notification_collapsed);
        Intent pauseIntent=new Intent("com.svc.toandx.mediaplayer.PlayService");
        pauseIntent.putExtra("action",ACTION_PAUSE);
        Intent exitIntent=new Intent("com.svc.toandx.mediaplayer.PlayService");
        exitIntent.putExtra("action",ACTION_EXIT);
        Intent nextSongIntent=new Intent("com.svc.toandx.mediaplayer.PlayService");
        nextSongIntent.putExtra("action",ACTION_NEXT);
        Intent prevSongIntent=new Intent("com.svc.toandx.mediaplayer.PlayService");
        prevSongIntent.putExtra("action",ACTION_PREV);
        PendingIntent pausePendingIntent = PendingIntent.getBroadcast(this,ACTION_PAUSE,pauseIntent,0);
        collapsedView.setOnClickPendingIntent(R.id.btn_pause_noti,pausePendingIntent);
        collapsedView.setOnClickPendingIntent(R.id.btn_exit_noti,PendingIntent.getBroadcast(this,ACTION_EXIT,exitIntent,0));
        collapsedView.setOnClickPendingIntent(R.id.btn_next_noti,PendingIntent.getBroadcast(this,ACTION_NEXT,nextSongIntent,0));
        collapsedView.setOnClickPendingIntent(R.id.btn_prev_noti,PendingIntent.getBroadcast(this,ACTION_PREV,prevSongIntent,0));
        createNotificationChannel();
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);
        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.icon)
                .setCustomContentView(collapsedView)
                .setCustomBigContentView(collapsedView)
                .setContentIntent(pendingIntent)
                .setSound(null)
                .build();
        startForeground(NOTIFY_ID, notification);
        notificationManager.notify(NOTIFY_ID,notification);
        handler=new Handler();
        runnable=new Runnable() {
            @Override
            public void run() {
                Intent msgIntent= new Intent("com.svc.toandx.mediaplayer.MainActivity");
                msgIntent.putExtra("action",ACTION_PROGRESS);
                msgIntent.putExtra("progress",mediaPlayer.getCurrentPosition());
                sendBroadcast(msgIntent);
                handler.postDelayed(this,100);
            }
        };
        startPlayService(intent);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        configMediaSession();
        return START_NOT_STICKY;
    }
    public void updateNotify()
    {
        notificationManager.notify(NOTIFY_ID,notification);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaSessionCompat!=null) mediaSessionCompat.release();
        unregisterReceiver(receiver);
        unregisterReceiver(becomingNoisyReceive);
        mediaPlayer.pause();
        handler.removeCallbacks(runnable);
        stopForeground(true);
        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
        replyIntent = new Intent("com.svc.toandx.mediaplayer.MainActivity");
        replyIntent.putExtra("action",ACTION_EXIT);
        sendBroadcast(replyIntent);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        mediaSessionCompat.release();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setSound(null,null);
            //NotificationManager manager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }
}
//https://codinginflow.com/tutorials/android/custom-notification
//https://androidwave.com/foreground-service-android-example/
