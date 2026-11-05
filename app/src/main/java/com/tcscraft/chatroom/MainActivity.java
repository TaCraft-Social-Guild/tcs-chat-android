package com.tcscraft.chatroom;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private SharedPreferences prefs;
    private LinearLayout rootLayout;
    private static final String PREFS_NAME = "TCSChatPrefs";
    private static final String KEY_SAVED = "saved_key";
    private static final String BASE_URL = "https://www.tcscraft.com/chat/";
    private Handler handler = new Handler(Looper.getMainLooper());
    private int lastKeyboardHeight = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        rootLayout = findViewById(R.id.rootLayout);
        Button refreshBtn = findViewById(R.id.refreshBtn);
        Button clearDataBtn = findViewById(R.id.clearDataBtn);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        setupWebView();
        setupKeyboardListener();
        webView.loadUrl(BASE_URL);

        refreshBtn.setOnClickListener(v -> {
            webView.loadUrl(BASE_URL);
        });

        clearDataBtn.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            webView.clearCache(true);
            webView.clearHistory();
            webView.clearFormData();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.evaluateJavascript("localStorage.clear();", null);
                webView.evaluateJavascript("sessionStorage.clear();", null);
            } else {
                webView.loadUrl("javascript:localStorage.clear();");
                webView.loadUrl("javascript:sessionStorage.clear();");
            }
            webView.loadUrl(BASE_URL);
            Toast.makeText(MainActivity.this, "已清除所有数据，请重新登录", Toast.LENGTH_LONG).show();
        });
    }

    private void setupKeyboardListener() {
        final View activityRoot = findViewById(android.R.id.content);
        ViewTreeObserver.OnGlobalLayoutListener layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            private final Rect r = new Rect();
            private final int visualDiffThreshold = 200;

            @Override
            public void onGlobalLayout() {
                activityRoot.getWindowVisibleDisplayFrame(r);
                int screenHeight = activityRoot.getRootView().getHeight();
                int keyboardHeight = screenHeight - r.bottom;

                if (Math.abs(keyboardHeight - lastKeyboardHeight) < 50) return;

                lastKeyboardHeight = keyboardHeight;

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) webView.getLayoutParams();
                if (keyboardHeight > visualDiffThreshold) {
                    params.bottomMargin = keyboardHeight;
                } else {
                    params.bottomMargin = 0;
                }
                webView.setLayoutParams(params);

                if (keyboardHeight > visualDiffThreshold) {
                    handler.postDelayed(() -> {
                        webView.loadUrl("javascript:var input=document.getElementById('messageInput');if(input){input.scrollIntoView({behavior:'smooth',block:'center'});}");
                    }, 150);
                }
            }
        };
        activityRoot.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(MainActivity.this, "加载失败: " + description, Toast.LENGTH_SHORT).show();
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
            handler.postDelayed(() -> webView.loadUrl(BASE_URL), 200);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
            webView.pauseTimers();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            webView.clearHistory();
            webView.destroy();
        }
        super.onDestroy();
    }
}