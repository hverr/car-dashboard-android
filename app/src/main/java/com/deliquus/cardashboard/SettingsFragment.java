package com.deliquus.cardashboard;

import android.content.*;
import android.os.*;
import android.preference.*;
import android.view.*;

public class SettingsFragment extends PreferenceFragment {
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }

    @Override
    public void onStart() {
        super.onStart();
        preferenceListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences prefs, String s) {
                if (s.equals(PreferenceKeys.RPI_ADDRESS_PREF)) {
                    updateRPiAddress(prefs);
                }
            }
        };
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener);
        updateRPiAddress(prefs);
    }

    @Override
    public void onStop() {
        super.onStop();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener);
    }

    private void updateRPiAddress(SharedPreferences prefs) {
        Preference pref = findPreference(PreferenceKeys.RPI_ADDRESS_PREF);
        pref.setSummary(prefs.getString(PreferenceKeys.RPI_ADDRESS_PREF, null));
    }
}
