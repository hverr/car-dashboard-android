package com.deliquus.cardashboard;

import android.os.*;
import android.support.v7.app.*;
import android.webkit.*;

public class MainActivity extends AppCompatActivity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = (WebView)findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return false;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        webView.loadUrl("https://google.com");
    }

    @Override
    protected void onStop() {
        super.onStop();
        webView.loadUrl("about:blank");
    }
}
