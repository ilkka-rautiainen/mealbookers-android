package fi.datahiiri.mealbookers;

import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

/**
 * Created by Lisa och Ilkka on 26.2.2015.
 */
public class WebViewInterface {
    private MainActivity mActivity;

    public WebViewInterface(MainActivity activity) {
        this.mActivity = activity;
    }

    @JavascriptInterface
    public void onLogin() {
        Log.i("WebViewInterface", "Got login");
        mActivity.onLogin();
    }

    @JavascriptInterface
    public void onLogout() {
        Log.i("WebViewInterface", "Got logout");
        mActivity.onLogout();
    }

    @JavascriptInterface
    public void isLoggedIn(boolean state) {
        Log.i("WebViewInterface", "Got logged in state: " + Boolean.toString(state));
        mActivity.saveLoggedInState(state);
        mActivity.invalidateOptionsMenu();
    }

    @JavascriptInterface
    public void alert(String text) {
        Toast.makeText(mActivity, text, Toast.LENGTH_SHORT).show();
    }
}
