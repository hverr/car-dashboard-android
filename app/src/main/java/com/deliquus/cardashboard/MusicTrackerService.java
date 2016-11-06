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

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

import org.json.*;

public class MusicTrackerService extends Service {
    static private final String TAG = "MusicTrackerService";
    static private final int NOTIFICATION_ID = 1;
    static public final int PERMISSIONS_REQUEST_CODE = 1;
    static private final int URL_CONNECTION_TIMEOUT = 2000;

    static public final String RPI_ADDRESS_KEY = "rpi_address_key";
    static public final String RPI_PORT_KEY = "rpi_address_port";

    static private final String MUSIC_PLAYSTATE_CHANGED = "com.android.music.playstatechanged";
    static private final String MUSIC_META_CHANGED = "com.android.music.metachanged";

    private BroadcastReceiver broadcastReceiver;
    private MusicTrackerThread musicTrackerThread;
    private SynchronousQueue<MusicTrackerEvent> musicTrackerEvents = new SynchronousQueue<>();

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
                Long position = null;
                if(intent.hasExtra("position")) {
                    position = intent.getLongExtra("position", 0);
                }
                String track = intent.getStringExtra("track");
                String artist = intent.getStringExtra("artist");
                boolean playing = intent.getBooleanExtra("playing", false);

                String filePath = queryMediaPath(id);
                try {
                    musicTrackerEvents.put(new MusicTrackerEvent(id, filePath, playing, track, artist, position));
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
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
            musicTrackerThread = new MusicTrackerThread(musicTrackerEvents, address, port);
            musicTrackerThread.start();
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
        musicTrackerThread.stopAndWait();
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
        private boolean stopped;

        private Integer lastTrackData = null;

        private final SynchronousQueue<MusicTrackerEvent> events;

        MusicTrackerThread(SynchronousQueue<MusicTrackerEvent> events, String address, String port) {
            this.events = events;
            this.address = address;
            this.port = port;
        }

        @Override
        public void run() {
            try {
                while (!stopped) {
                    nextEvent(events.take());
                }
            } catch(InterruptedException e) {
                musicTrackerThread = null;
            }
        }

        private void nextEvent(MusicTrackerEvent event) {
            android.util.Log.d(TAG, "nextEvent(" + event + ")");

            try {
                sendMetaData(event);
                if (event.playing == true && (lastTrackData == null || lastTrackData != event.songId)) {
                    sendTrackData(event);
                    lastTrackData = event.songId;
                }
            } catch(ServerException | IOException e) {
                android.util.Log.e(TAG, "nextEvent(): " + e);
            }
        }

        private void sendMetaData(MusicTrackerEvent event) throws ServerException, IOException {
            android.util.Log.d(TAG, "sendMetaData(" + event + ")");

            byte[] body = event.metadataJSON().toString().getBytes("utf-8");
            URL url = new URL("http://" + getAddress() + ":" + getPort() + "/api/music/metadata");
            android.util.Log.d(TAG, "sendMetaData(): Posting to " + url);
            HttpURLConnection c = (HttpURLConnection)url.openConnection();
            try {
                c.setConnectTimeout(URL_CONNECTION_TIMEOUT);
                c.setDoOutput(true);
                c.setFixedLengthStreamingMode(body.length);
                c.addRequestProperty("Content-Type", "application/json");

                OutputStream out = new BufferedOutputStream(c.getOutputStream());
                out.write(body);
                out.flush();

                c.connect();

                int statusCode = c.getResponseCode();
                if(statusCode != HttpURLConnection.HTTP_OK) {
                    throw new ServerException("Could not send metadata: Response " + statusCode + " " + c.getResponseMessage());
                }
            } finally {
                c.disconnect();
            }
        }

        private void sendTrackData(MusicTrackerEvent event) throws ServerException, IOException {
            android.util.Log.d(TAG, "sendTrackData(" + event + ")");

            File file = new File(event.filePath);
            BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
            try {
                URL url = new URL("http://" + getAddress() + ":" + getPort() + "/api/music/track_data/" + event.songId + "/" + getFileExtension(file, "data"));
                android.util.Log.d(TAG, "sendTrackData(): Posting " + event.filePath + " to " + url);
                HttpURLConnection c = (HttpURLConnection)url.openConnection();
                try {
                    c.setConnectTimeout(URL_CONNECTION_TIMEOUT);
                    c.setDoOutput(true);
                    c.setChunkedStreamingMode(0);
                    c.addRequestProperty("Content-Type", "application/octet-stream");

                    BufferedOutputStream out = new BufferedOutputStream(c.getOutputStream());
                    int read;
                    byte[] buf = new byte[4096];
                    while ((read = fis.read(buf)) > 0) {
                        out.write(buf, 0, read);
                        out.flush();
                    }

                    c.connect();

                    int statusCode = c.getResponseCode();
                    if(statusCode != HttpURLConnection.HTTP_OK) {
                        throw new ServerException("Could not send metadata: Response " + statusCode + " " + c.getResponseMessage());
                    }

                } finally {
                    c.disconnect();
                }
            } finally {
                fis.close();
            }
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

        synchronized public void stopAndWait() {
            this.stopped = true;
            musicTrackerThread.interrupt();
            try {
                musicTrackerThread.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            musicTrackerThread = null;
        }
    }

    static private class MusicTrackerEvent {
        public final int songId;
        public final String filePath;
        public final Boolean playing;
        public final String track;
        public final String artist;
        public final Long position;

        public MusicTrackerEvent(int songId, String filePath, Boolean playing, String track, String artist, Long position) {
            this.songId = songId;
            this.filePath = filePath;
            this.playing = playing;
            this.track = track;
            this.artist = artist;
            this.position = position;
        }

        public JSONObject metadataJSON() {
            try {
                return new JSONObject()
                        .put("songId", songId)
                        .put("playing", playing)
                        .put("track", orNull(track))
                        .put("artist", orNull(artist))
                        .put("position", position == null ? JSONObject.NULL : position.intValue());
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        private Object orNull(Object o) {
            return o == null ? JSONObject.NULL : o;
        }

        @Override
        public String toString() {
            return "MusicTrackerEvent{" +
                    "songId=" + songId +
                    ", filePath='" + filePath + '\'' +
                    ", playing=" + playing +
                    ", track='" + track + '\'' +
                    ", artist='" + artist + '\'' +
                    ", position=" + position +
                    '}';
        }
    }

    static private class ServerException extends Exception {
        public ServerException(String msg) {
            super(msg);
        }
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

    static private String getFileExtension(File f, String def) {
        String n = f.getName();
        int i = n.lastIndexOf(".");
        if(i > 0) {
            return n.substring(i+1);
        }
        return def;
    }
}
