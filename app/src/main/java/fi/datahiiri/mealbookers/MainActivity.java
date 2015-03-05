package fi.datahiiri.mealbookers;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import fi.datahiiri.mealbookers.exceptions.ForbiddenException;
import fi.datahiiri.mealbookers.fi.datahiiri.mealbookers.service.MealbookersGateway;


public class MainActivity extends ActionBarActivity {

    private static final int REQUEST_CODE_LOGIN = 1;
    public static final int REQUEST_CODE_NOTIFICATION_NOACCEPT = 2;
    public static final int REQUEST_CODE_NOTIFICATION_ACCEPT = 3;
    public static final int NOTIFICATION_ID = 1;

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";

    /**
     * Tag used on log messages.
     */
    static final String TAG = "Mealbookers";

    /**
     * Substitute you own sender ID here. This is the project number you got
     * from the API Console, as described in "Getting Started."
     */
    String SENDER_ID = "481971064112";

    private WebView mWebView;
    private ProgressBar mProgressBar;
    private Menu mMenu;

    GoogleCloudMessaging gcm;
    String regid;
    Context context;
    private boolean webViewLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        setupActionBar();

        // Catch notification click
        onNewIntent(getIntent());

        startMealbookersService();

        // Check device for Play Services APK. If check succeeds, proceed with
        //  GCM registration.
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context);

            if (regid.isEmpty()) {
                registerInBackground();
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }

        mWebView = (WebView) findViewById(R.id.webView);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mWebView.addJavascriptInterface(new WebViewInterface(this), "AndroidApp");
        }

        // Make sure logged in state is taken into account when menu is inflated
        resumeLoggedInState();

        // Check backend login trafic status if logged in
        if (isLoggedIn()) {
            new CheckServiceLoginState().execute();
        }
    }

    /**
     * Starts the web view.
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (!webViewLoading) {
            initializeWebView();
        }
    }

    /**
     * Does the actions when activity is stopped. Clears the vew view.
     */
    @Override
    protected void onStop() {
        super.onStop();
        mWebView.loadUrl("about:blank");
    }

    /**
     * Setups the action bar
     */
    private void setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    /**
     * Initializes the web view for the first time when the app is opened
     */
    private void initializeWebView() {

        // Progress bar
        final Activity activity = this;
        mProgressBar.setVisibility(View.VISIBLE);

        mWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                if (progress < 80) {
                    mProgressBar.setVisibility(View.VISIBLE);
                }
                //Make the bar disappear after URL is loaded, and changes string to Loading...
//                MainActivity.setTitle("Loading...");
                mProgressBar.setProgress((int) Math.round(((double) progress / 100.0) * (double) mProgressBar.getMax()));

                // Return the app name after finish loading
                if (progress == 100) {
                    mProgressBar.setVisibility(View.GONE);
                    webViewLoading = false;
                }
            }
        });

        mWebView.clearCache(true);
        mWebView.loadUrl("http://mealbookers.net/#/menu/today?source=android");
        webViewLoading = true;
    }

    /**
     * Opens the web view with a login request.
     * @param email
     * @param password
     */
    private void loginToMealbookersWebView(String email, String password) {
        Log.i(TAG, "Logging in to vew view");
        try {
            mWebView.clearCache(true);
            mWebView.loadUrl("about:blank");
            mWebView.loadUrl("http://mealbookers.net/#/menu/today?source=android"
                            + "&email=" + URLEncoder.encode(email, "UTF-8")
                            + "&password=" + URLEncoder.encode(password, "UTF-8")
            );
            webViewLoading = true;
//            Toast.makeText(getApplicationContext(), R.string.sign_in_successful, Toast.LENGTH_LONG).show();
//            startMealbookersService();

            registerInBackground();
        }
        catch (UnsupportedEncodingException e) {
            Log.e("loginToMealbookersWebView", "encoding error", e);
            Toast.makeText(getApplicationContext(), R.string.sign_in_failed, Toast.LENGTH_SHORT).show();
        }
    }
    private void acceptSuggestionInMealbookersWebView(String token) {
        mWebView.clearCache(true);
        mWebView.loadUrl("about:blank");
        Log.d("acceptSuggestionInMealbookersWebView", "http://mealbookers.net/#/menu/suggestion/accept/" + token + "?source=android");
        mWebView.loadUrl("http://mealbookers.net/#/menu/suggestion/accept/" + token + "?source=android");
        webViewLoading = true;
    }

    /**
     * Starts the background service.
     */
    private void startMealbookersService() {
//        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//
//        Intent alarmIntent = new Intent(this, MealbookersService.class);
//
//        PendingIntent pending = PendingIntent.getService(this, 0, alarmIntent, 0);
//
//        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
//                SystemClock.elapsedRealtime() + 10 * 1000, pending);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        mMenu = menu;
        doMenuChanges();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_login) {
            openLogin();
            return true;
        }
        else if (id == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Opens login activity
     */
    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, REQUEST_CODE_LOGIN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("onActivityResult", "got request code" + Integer.toString(resultCode));
        switch(requestCode) {
            case (REQUEST_CODE_LOGIN) : {
                if (resultCode == Activity.RESULT_OK) {
                    String email = data.getStringExtra("email");
                    String password = data.getStringExtra("password");
                    loginToMealbookersWebView(email, password);
                }
                break;
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        int requestCode = intent.getIntExtra("requestCode", -1);
        Log.d("onNewIntent", "Got intent with " + Integer.toString(requestCode));
        cancelNotification(this, NOTIFICATION_ID);
        if (requestCode == REQUEST_CODE_NOTIFICATION_ACCEPT) {
            acceptSuggestionInMealbookersWebView(intent.getStringExtra("token"));
        }
    }

    public static void cancelNotification(Context ctx, int notifyId) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) ctx.getSystemService(ns);
        nMgr.cancel(notifyId);
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device checkPlayServices is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Gets the current registration ID for application on GCM service.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing registration ID is not guaranteed to work with
        // the new app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the registration ID in your app is up to you.
        return getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg;
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    // You should send the registration ID to your server over HTTP,
                    // so it can use GCM/HTTP or CCS to send messages to your app.
                    // The request to your server should be authenticated if your app
                    // is using accounts.
                    sendRegistrationIdToBackend();

                    // For this demo: we don't need to send it because the device
                    // will send upstream messages to a server that echo back the
                    // message using the 'from' address in the message.

                    // Persist the registration ID - no need to register again.
                    storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
//                mDisplay.append(msg + "\n");
                Log.i(TAG, "GCM Registration: " + msg);
            }
        };
        task.execute(null, null, null);
    }

    /**
     * Stores the registration ID and app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP
     * or CCS to send messages to your app. Not needed for this demo since the
     * device sends upstream messages to a server that echoes back the message
     * using the 'from' address in the message.
     */
    private void sendRegistrationIdToBackend() {
        try {
            MealbookersGateway.sendGCMRegid(regid, context);
        } catch (ForbiddenException e) {
            Log.i(TAG, "Not logged in to backend, unable to send gcm regid");
        } catch (Exception e) {
            Log.e(TAG, "Sending GCM regid to backend failed", e);
        }
    }

    /**
     * Logs the user out. Called from menu.
     */
    private void logout() {
        mWebView.clearCache(true);
        mWebView.loadUrl("about:blank");
        mWebView.loadUrl("http://mealbookers.net/#/menu/today?source=android&logout=1");
        onLogout();
    }

    /**
     * Makes differences in UI and saves the state.
     */
    public void onLogin() {
        Log.i("Menu", "logged in");
        invalidateOptionsMenu();
        saveLoggedInState(true);
    }

    /**
     * Makes differences in UI and saves the state. Called from Web View.
     */
    public void onLogout() {
        Log.i("Menu", "logged out");
        invalidateOptionsMenu();
        MealbookersGateway.logout(getApplicationContext());
        saveLoggedInState(false);
    }

    /**
     * Does changes to menu
     */
    private void doMenuChanges() {
        Log.d("Menu", "doMenuChanges");
        if (isLoggedIn()) {
            doLoginUIChanges();
        }
        else {
            doLogoutUIChanges();
        }
    }

    /**
     * Does the UI (menu) changes that are needed when logged in
     */
    private void doLoginUIChanges() {
        Log.d("Menu", "doLoginUIChanges start");
        if (mMenu != null) {
            Log.d("Menu", "doLoginUIChanges do");
            mMenu.findItem(R.id.action_login).setVisible(false);
            mMenu.findItem(R.id.action_logout).setVisible(true);
        }
    }

    /**
     * Does the UI (menu) changes that are needed when logged out
     */
    private void doLogoutUIChanges() {
        Log.d("Menu", "doLogoutUIChanges start");
        if (mMenu != null) {
            Log.d("Menu", "doLogoutUIChanges do");
            mMenu.findItem(R.id.action_login).setVisible(true);
            mMenu.findItem(R.id.action_logout).setVisible(false);
        }
    }

    /**
     * Saves logged in state
     * @param state
     */
    public void saveLoggedInState(boolean state) {
        getPreferences(context).edit().putBoolean("logged-in-state", state).commit();
    }

    /**
     * Resumes from the logged in state
     */
    private void resumeLoggedInState() {
        Log.d("Menu", "resumeLoggedInState");
        invalidateOptionsMenu();
    }

    private boolean isLoggedIn() {
        return getPreferences(context).getBoolean("logged-in-state", false);
    }

    /**
     * Checks from the backend if the service traffic is logged in. If not -> log out web view too.
     */
    public class CheckServiceLoginState extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                MealbookersGateway.getUser(MainActivity.this);
                return 1;
            }
            catch (ForbiddenException e) {
                return 0;
            }
            catch (Exception e) {
                return 1;
            }
        }

        @Override
        protected void onPostExecute(final Integer result) {
            if (result == 0) {
                Log.i(TAG, "Service trafic got HTTP 403 -> logging out");
                logout();
            }
        }
    }
}
