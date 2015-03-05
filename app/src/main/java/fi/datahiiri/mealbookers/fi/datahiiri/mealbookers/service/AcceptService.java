package fi.datahiiri.mealbookers.fi.datahiiri.mealbookers.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import fi.datahiiri.mealbookers.MainActivity;
import fi.datahiiri.mealbookers.exceptions.ForbiddenException;

/**
 * Created by Lisa och Ilkka on 5.3.2015.
 */
public class AcceptService extends IntentService {
    Handler mHandler;

    public AcceptService() {
        super("AcceptService");
        mHandler = new Handler();
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        String token = intent.getStringExtra("token");
//        if (token == null)
//            token = this.getSharedPreferences(LoginActivity.PREFERENCES_FILENAME, Context.MODE_PRIVATE).getString("accept-suggestion-token", "");

        if (token.equals("")) {
            mHandler.post(new DisplayToast(this, "Accepting the suggestion failed"));
            return;
        }

        Log.d("AcceptService.onHandleIntent", "token: " + token);

        try {
            MealbookersGateway.acceptSuggestion(token, this);
            mHandler.post(new DisplayToast(this, "Suggestion accepted"));
        }
        catch (ForbiddenException e) {
            Log.e("AcceptService.onHandleIntent", "forbidden", e);
            mHandler.post(new DisplayToast(this, "You've been logged out"));
        }
        catch (Exception e) {
            Log.e("AcceptService.onHandleIntent", "error", e);
            mHandler.post(new DisplayToast(this, "Accepting the suggestion failed"));
        }
        MainActivity.cancelNotification(this, MainActivity.NOTIFICATION_ID);
        closeStatusbar();
    }

    private void closeStatusbar() {
        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        this.sendBroadcast(it);
    }
}
