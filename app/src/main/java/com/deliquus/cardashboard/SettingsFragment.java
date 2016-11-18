package com.deliquus.cardashboard;

import android.content.*;
import android.os.*;
import android.preference.*;

public class SettingsFragment extends PreferenceFragment {
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceListener;

    private BroadcastReceiver stopMusicStreamingReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }

    @Override
    public void onStart() {
        super.onStart();

        stopMusicStreamingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SwitchPreference pref = (SwitchPreference)findPreference(PreferenceKeys.MUSIC_STREAMING_PREF);
                pref.setChecked(false);
            }
        };
        getActivity().registerReceiver(stopMusicStreamingReceiver, new IntentFilter(MusicTrackerService.STOP_MUSIC_STREAMING_ACTION));

        preferenceListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences prefs, String s) {
                if(s.equals(PreferenceKeys.RPI_ADDRESS_PREF)) {
                    updateRPiAddress(prefs);
                } else if(s.equals(PreferenceKeys.RPI_PORT_PREF)) {
                    updateRPiPort(prefs);
                }
            }
        };
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener);
        updateRPiAddress(prefs);
        updateRPiPort(prefs);
        updateMusicStreaming();

        findPreference(PreferenceKeys.MUSIC_STREAMING_PREF).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                setMusicStreaming(prefs, (Boolean)o);
                return true;
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();

        getActivity().unregisterReceiver(stopMusicStreamingReceiver);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener);
    }

    private void updateRPiAddress(SharedPreferences prefs) {
        Preference pref = findPreference(PreferenceKeys.RPI_ADDRESS_PREF);
        pref.setSummary(prefs.getString(PreferenceKeys.RPI_ADDRESS_PREF, null));
    }

    private void updateRPiPort(SharedPreferences prefs) {
        Preference pref = findPreference(PreferenceKeys.RPI_PORT_PREF);
        pref.setSummary(prefs.getString(PreferenceKeys.RPI_PORT_PREF, null));
    }

    private void updateMusicStreaming() {
        SwitchPreference pref = (SwitchPreference)findPreference(PreferenceKeys.MUSIC_STREAMING_PREF);
        pref.setChecked(MusicTrackerService.isRunning(getActivity()));
    }

    private void setMusicStreaming(SharedPreferences prefs, boolean newValue) {
        if(newValue) {
            MusicTrackerService.start(getActivity(), prefs);
        } else {
            MusicTrackerService.stop(getActivity());
        }
    }
}
