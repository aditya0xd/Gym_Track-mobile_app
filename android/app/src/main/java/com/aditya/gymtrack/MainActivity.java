//package com.aditya.gymtrack;
//
//import android.os.Bundle;
//import android.webkit.CookieManager;
//import android.webkit.WebView;
//import com.getcapacitor.BridgeActivity;
//
//public class MainActivity extends BridgeActivity {
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        WebView webView = this.bridge.getWebView();
//        CookieManager.getInstance().setAcceptCookie(true);
//        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
//    }
//}

package com.aditya.gymtrack;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import com.getcapacitor.Bridge;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.BridgeWebViewClient;

public class MainActivity extends BridgeActivity {

    private static final String REMOTE_URL = "https://gym-track-sigma-ten.vercel.app/";
    private static final String OFFLINE_URL = "file:///android_asset/www/offline.html";

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean showingOffline = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webView = this.bridge.getWebView();
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        Bridge bridgeRef = this.bridge;
        webView.setWebViewClient(new BridgeWebViewClient(bridgeRef) {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    showingOffline = true;
                    view.loadUrl(OFFLINE_URL);
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
            public void onAvailable(Network network) {
                runOnUiThread(() -> {
                    if (showingOffline) {
                        showingOffline = false;
                        webView.loadUrl(REMOTE_URL);
                    }
                });
            }
        };

        connectivityManager.registerNetworkCallback(request, networkCallback);
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
}