package com.aditya.gymtrack;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.splashscreen.SplashScreen;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.BridgeWebViewClient;

public class MainActivity extends BridgeActivity {

    private static final String REMOTE_URL = "https://gym-track-sigma-ten.vercel.app/";
    private static final String REMOTE_HOST = "gym-track-sigma-ten.vercel.app";

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean showingOffline = false;
    private FrameLayout offlineOverlay;
    private WebView webView;

    private static final int ERROR_HOST_LOOKUP = -2;
    private static final int ERROR_CONNECT = -6;
    private static final int ERROR_TIMEOUT = -8;
    private static final int ERROR_IO = -7;
    private static final int ERROR_UNKNOWN = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        webView = this.bridge.getWebView();
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        offlineOverlay = buildOfflineOverlay();
        offlineOverlay.setVisibility(View.GONE);
        
        // Add overlay directly to the activity content view to ensure it's on top and avoid null parent issues
        addContentView(offlineOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        webView.setWebViewClient(new BridgeWebViewClient(this.bridge) {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);

                boolean isMainDocument = request.isForMainFrame()
                        && request.getUrl() != null
                        && REMOTE_HOST.equalsIgnoreCase(request.getUrl().getHost());

                boolean isConnectivityError = isLikelyConnectivityError(error.getErrorCode());

                if (isMainDocument && isConnectivityError) {
                    showOffline();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url != null && url.contains(REMOTE_HOST) && showingOffline) {
                    showingOffline = false;
                    offlineOverlay.setVisibility(View.GONE);
                }
            }
        });

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(() -> {
                    if (showingOffline) {
                        hideOfflineAndReload();
                    }
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> {
                    showOffline();
                });
            }
        };

        connectivityManager.registerNetworkCallback(request, networkCallback);
    }

    private boolean isLikelyConnectivityError(int errorCode) {
        switch (errorCode) {
            case ERROR_HOST_LOOKUP:
            case ERROR_CONNECT:
            case ERROR_TIMEOUT:
            case ERROR_IO:
            case ERROR_UNKNOWN:
                return true;
            default:
                return false;
        }
    }

    private void showOffline() {
        showingOffline = true;
        offlineOverlay.setVisibility(View.VISIBLE);
    }

    private void hideOfflineAndReload() {
        showingOffline = false;
        offlineOverlay.setVisibility(View.GONE);
        webView.loadUrl(REMOTE_URL);
    }

    private FrameLayout buildOfflineOverlay() {
        FrameLayout container = new FrameLayout(this);
        container.setBackgroundColor(Color.parseColor("#0f0f10"));
        container.setClickable(true);
        container.setFocusable(true);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams boxParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        box.setPadding(60, 60, 60, 60);

        TextView title = new TextView(this);
        title.setText("No internet connection");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);

        TextView message = new TextView(this);
        message.setText("GymTrack needs an internet connection to load your data. We'll reconnect automatically once you're back online.");
        message.setTextColor(Color.parseColor("#aaaaaa"));
        message.setTextSize(14);
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, 20, 0, 40);

        Button retryButton = new Button(this);
        retryButton.setText("Retry now");
        retryButton.setTextColor(Color.parseColor("#062b0f"));
        retryButton.setBackgroundColor(Color.parseColor("#22c55e"));
        retryButton.setOnClickListener(v -> hideOfflineAndReload());

        box.addView(title);
        box.addView(message);
        box.addView(retryButton);
        container.addView(box, boxParams);

        return container;
    }

    @Override
    public void onDestroy() {
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (IllegalArgumentException ignored) {
            }
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (showingOffline) {
            // Close the app if they try to go back while offline
            finish();
            return;
        }
        // If online, let Capacitor and the Next.js app handle the back button event
        super.onBackPressed();
    }
}
