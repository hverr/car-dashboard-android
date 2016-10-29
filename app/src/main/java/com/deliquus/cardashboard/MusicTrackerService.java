package com.deliquus.cardashboard;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.*;
import android.os.*;

public class MusicTrackerService extends Service {
    static private final String TAG = "MusicTrackerService";
    static private final int NOTIFICATION_ID = 1;

    static public final String RPI_ADDRESS_KEY = "rpi_address_key";
    static public final String RPI_PORT_KEY = "rpi_address_port";

    static private final String MUSIC_PLAYSTATE_CHANGED = "com.android.music.playstatechanged";
    static private final String MUSIC_META_CHANGED = "com.android.music.metachanged";
    static private final String MUSIC_QUEUE_CHANGED = "com.android.music.queuechanged";

    private BroadcastReceiver broadcastReceiver;
    private MusicTrackerThread musicTrackerThread;

    public MusicTrackerService() {

    }

    @Override
    public void onCreate() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(MUSIC_META_CHANGED);
        filter.addAction(MUSIC_PLAYSTATE_CHANGED);
        filter.addAction(MUSIC_QUEUE_CHANGED);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Uri uri = intent.getData();
                android.util.Log.d(TAG, "onReceive(): " + uri);
                android.util.Log.d(TAG, "onReceive(): Track: " + intent.getStringExtra("track"));
                android.util.Log.d(TAG, "onReceive(): Artist: " + intent.getStringExtra("artist"));
                android.util.Log.d(TAG, "onReceive(): Album: " + intent.getStringExtra("album"));
            }
        };
        registerReceiver(broadcastReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String address = intent.getStringExtra(RPI_ADDRESS_KEY);
        String port = intent.getStringExtra(RPI_PORT_KEY);
        if(address == null || port == null) {
            android.util.Log.e(TAG, "onStartCommand(): address and port should not be null");
            return START_NOT_STICKY;
        }

        android.util.Log.i(TAG, "onStartCommand(): " + address + ":" + port);

        if(musicTrackerThread == null) {
            Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_queue_music_black_24dp);
            Intent activityIntent = new Intent(this, MainActivity.class);
            Notification notification = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.ic_queue_music_black_24dp)
                    .setContentTitle("Streaming music to your RPi")
                    .setContentText("Click to open Car Dashboard")
                    .setOngoing(true)
                    .setContentIntent(PendingIntent.getActivity(this, 0, activityIntent, 0))
                    .build();
            startForeground(NOTIFICATION_ID, notification);
            musicTrackerThread = new MusicTrackerThread(address, port);
            //musicTrackerThread.start();
        } else {
            musicTrackerThread.setAddress(address);
            musicTrackerThread.setPort(port);
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        android.util.Log.d(TAG, "onDestroy()");
        unregisterReceiver(broadcastReceiver);
        stopForeground(true);
    }

    static public boolean isRunning(Context ctx) {
        ActivityManager manager = (ActivityManager)ctx.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MusicTrackerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private class MusicTrackerThread extends Thread {
        private String address;
        private String port;

        MusicTrackerThread(String address, String port) {
            this.address = address;
            this.port = port;
        }

        @Override
        public void run() {
            android.util.Log.d(TAG, "run()");
        }

        synchronized public void setAddress(String address) {
            this.address = address;
        }

        synchronized public String getAddress() {
            return address;
        }

        synchronized public void setPort(String port) {
            this.port = port;
        }

        synchronized public String getPort() {
            return port;
        }
    }

    static private class MusicTrackerEvent {
        public Uri uri;
        public Object shouldStop;
    }
}
