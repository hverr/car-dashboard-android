package com.deliquus.cardashboard;

import android.*;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.database.*;
import android.graphics.*;
import android.net.*;
import android.os.*;
import android.provider.*;
import android.support.v4.app.*;
import android.support.v4.content.*;

public class MusicTrackerService extends Service {
    static private final String TAG = "MusicTrackerService";
    static private final int NOTIFICATION_ID = 1;
    static public final int PERMISSIONS_REQUEST_CODE = 1;

    static public final String RPI_ADDRESS_KEY = "rpi_address_key";
    static public final String RPI_PORT_KEY = "rpi_address_port";

    static private final String MUSIC_PLAYSTATE_CHANGED = "com.android.music.playstatechanged";
    static private final String MUSIC_META_CHANGED = "com.android.music.metachanged";

    private BroadcastReceiver broadcastReceiver;
    private MusicTrackerThread musicTrackerThread;

    public MusicTrackerService() {

    }

    static public void start(Activity activity, SharedPreferences prefs) {
        int v = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        if(v != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.w(TAG, "No permission to query media files, asking the user");
            ActivityCompat.requestPermissions(activity, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, PERMISSIONS_REQUEST_CODE);
            return;
        }

        Intent intent = new Intent(activity, MusicTrackerService.class);
        intent.putExtra(MusicTrackerService.RPI_ADDRESS_KEY, prefs.getString(PreferenceKeys.RPI_ADDRESS_PREF, null));
        intent.putExtra(MusicTrackerService.RPI_PORT_KEY, prefs.getString(PreferenceKeys.RPI_PORT_PREF, null));
        activity.startService(intent);
    }

    static public void onRequestPermissionsResult(Activity activity, SharedPreferences prefs, String[] permissions, int[] grantResults) {
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            start(activity, prefs);
        } else {
            android.util.Log.e(TAG, "Not starting music tracker, permission denied");
        }
    }

    static public void stop(Activity activity) {
        Intent intent = new Intent(activity, MusicTrackerService.class);
        activity.stopService(intent);
    }

    @Override
    public void onCreate() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(MUSIC_META_CHANGED);
        filter.addAction(MUSIC_PLAYSTATE_CHANGED);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Uri uri = intent.getData();
                android.util.Log.d(TAG, "onReceive(): " + intent.getExtras());
                for(String key : intent.getExtras().keySet()) {
                    android.util.Log.d(TAG, "onReceive(): " + key + "=" + intent.getExtras().get(key));
                }

                if (!intent.hasExtra("id") || !intent.hasExtra("playing")) {
                    android.util.Log.e(TAG, "onReceive(): Not enough data to stream track");
                } else if (!intent.hasExtra("position")) {
                    android.util.Log.w(TAG, "onReceive(): Track position not available, streaming from beginning");
                }

                int id = intent.getIntExtra("id", -1);
                long position = intent.getLongExtra("position", 0);
                boolean playing = intent.getBooleanExtra("playing", false);

                String filePath = queryMediaPath(id);
                android.util.Log.d(TAG, "Song " + id + " (file=" + filePath + ", playing=" + playing + ", pos=" + position + ")");
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

    private String queryMediaPath(int id) {
        Uri media = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media._ID + "=?";
        String[] selectionArgs = { Integer.toString(id) };
        String[] projection = { MediaStore.Audio.Media.DATA };

        Cursor c = getContentResolver().query(media, projection, selection, selectionArgs, null);
        if(c != null) {
            while (c.moveToNext()) {
                int index = c.getColumnIndex(MediaStore.Audio.Media.DATA);
                if (index == -1) {
                    android.util.Log.e(TAG, "No data column for song with id " + id);
                } else {
                    String s = c.getString(index);
                    c.close();
                    return s;
                }
            }
            c.close();
        }
        android.util.Log.e(TAG, "No song found with id " + id);
        return null;
    }
}
