package com.deliquus.cardashboard;

import android.app.*;
import android.content.*;
import android.os.*;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.view.*;
import android.webkit.*;


public class DashboardFragment extends Fragment {
    static public final String TAG = "DashboardFragment";
    private WebView webView;

    private SharedPreferences.OnSharedPreferenceChangeListener preferenceListener;

    public DashboardFragment() {
        // Obligatory empty constructor
    }

    public static DashboardFragment newInstance() {
        Bundle args = new Bundle();
        DashboardFragment fragment = new DashboardFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        webView = (WebView)getView().findViewById(R.id.web_view);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        updateRPiAddress(prefs);
        preferenceListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences prefs, String s) {
                if (s.equals(PreferenceKeys.RPI_ADDRESS_PREF) || s.equals(PreferenceKeys.RPI_PORT_PREF)) {
                    updateRPiAddress(prefs);
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.dashboard_fragment_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.refresh:
                webView.reload();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateRPiAddress(SharedPreferences prefs) {
        String address = prefs.getString(PreferenceKeys.RPI_ADDRESS_PREF, null);
        String port = prefs.getString(PreferenceKeys.RPI_PORT_PREF, null);
        webView.loadUrl("http://" + address + ":" + port);
    }
}
