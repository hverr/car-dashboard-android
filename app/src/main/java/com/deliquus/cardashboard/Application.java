package com.deliquus.cardashboard;

import android.preference.*;

public class Application extends android.app.Application {
    static private final String TAG = "Application";

    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceManager.setDefaultValues(this, R.xml.settings, true);
        android.util.Log.d(TAG, "onCreate()");
    }
}
