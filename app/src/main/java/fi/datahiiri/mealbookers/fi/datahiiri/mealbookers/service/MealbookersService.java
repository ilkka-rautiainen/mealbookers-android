package fi.datahiiri.mealbookers.fi.datahiiri.mealbookers.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.util.Calendar;
import java.util.Date;

import fi.datahiiri.mealbookers.LoginActivity;
import fi.datahiiri.mealbookers.MainActivity;
import fi.datahiiri.mealbookers.R;
import fi.datahiiri.mealbookers.models.PushNotification;
import fi.datahiiri.mealbookers.models.PushNotificationResult;

/**
 * Created by Lisa och Ilkka on 18.2.2015.
 */
public class MealbookersService extends IntentService {
    Handler mHandler;

    public MealbookersService() {
        super("MealbookersService");
        mHandler = new Handler();
    }

    /**
     * Processes a call to the service. Sets a next wakeup only if there was a valid login information.
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            Log.d("MealbookersService", "intent handled");

            Bundle extras = intent.getExtras();
            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
            // The getMessageType() intent parameter must be the intent you received
            // in your BroadcastReceiver.
            String messageType = gcm.getMessageType(intent);

            if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
                if (GoogleCloudMessaging.
                        MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                    Log.e("MealbookersService", "Send error: " +
                            extras.toString());
                } else if (GoogleCloudMessaging.
                        MESSAGE_TYPE_DELETED.equals(messageType)) {
                    Log.i("MealbookersService", "Deleted messages on server: " +
                            extras.toString());
                    // If it's a regular GCM message, do some work.
                } else if (GoogleCloudMessaging.
                        MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                    // This loop represents the service doing some work.
//                    for (int i=0; i<5; i++) {
//                        Log.i(TAG, "Working... " + (i+1)
//                                + "/5 @ " + SystemClock.elapsedRealtime());
//                        try {
//                            Thread.sleep(5000);
//                        } catch (InterruptedException e) {
//                        }
//                    }
                    Log.i("MealbookersService", "received intent: " + extras.toString());

                    String pushNotificationString = extras.getString("notification");
                    PushNotification pushNotification = MealbookersGateway.getGson().fromJson(pushNotificationString, PushNotification.class);
                    if (pushNotification == null) {
                        Log.e("MealbookersService", "Push notification was null");
                    }
                    else {
                        MealbookersGateway.markNotificationReceived(pushNotification.id, getApplicationContext());
                        sendNotification(pushNotification);
                    }
                    // Post notification of received message.
//                    sendNotification("Received: " + extras.toString());
//                    Log.i(TAG, "Received: " + extras.toString());
                }
            }
            // Release the wake lock provided by the WakefulBroadcastReceiver.
            GcmBroadcastReceiver.completeWakefulIntent(intent);

//            final SharedPreferences prefs = this.getSharedPreferences(LoginActivity.PREFERENCES_FILENAME, Context.MODE_PRIVATE);
//            int after = prefs.getInt("sync-time", 0);
//
//            PushNotificationResult notificationResult = MealbookersGateway.getNotifications(after, this);
//            if (notificationResult != null) {
//                showNotifications(notificationResult);
//                saveSyncTime(notificationResult.time);
//                setNextAlert();
//            }
        }
//        catch (ForbiddenException e) {
//            Log.e("MealbookersService.onHandleIntent", "got forbidden from server");
//        }
        catch (Exception e) {
            Log.e("MealbookersService.onHandleIntent", "processing failed", e);
//            mHandler.post(new DisplayToast(this, "Fetching Mealbookers notifications failed"));
            setNextAlert();
        }
    }

    /**
     * Saves the last sync time to local preferences
     * @param time
     */
    private void saveSyncTime(int time) {
        final SharedPreferences prefs = this.getSharedPreferences(LoginActivity.PREFERENCES_FILENAME, Context.MODE_PRIVATE);
        prefs.edit().putInt("sync-time", time).apply();
    }

    private Integer getLastSyncTime() {
        final SharedPreferences prefs = this.getSharedPreferences(LoginActivity.PREFERENCES_FILENAME, Context.MODE_PRIVATE);
        int syncTime = prefs.getInt("sync-time", -1);
        if (syncTime == -1) {
            return null;
        }
        else {
            return syncTime;
        }
    }

    /**
     * Shows notifications
     * @param notificationResult
     */
    private void showNotifications(PushNotificationResult notificationResult) {
        Integer takeNewerThan = getLastSyncTime();

        // Take only this old notifications
        int earliestAcceptedNotificationTime = Math.round(new Date().getTime() / 1000) - LoginActivity.NOTIFICATION_LIFETIME;
        if (takeNewerThan == null || takeNewerThan < earliestAcceptedNotificationTime) {
            takeNewerThan = earliestAcceptedNotificationTime;
        }

        for (PushNotification pushNotification : notificationResult.notifications) {
            if (pushNotification.time > takeNewerThan) {
                if (pushNotification.type == PushNotification.NOTIFICATION_TYPE_SUGGEST && pushNotification.suggestion == null) {
                    continue;
                }
                Log.d("showNotifications", "showing notification of type" + Integer.toString(pushNotification.type));
                sendNotification(pushNotification);
            }
        }
    }

    /**
     * Sets next wakeup for the service according to the time interval.
     */
    private void setNextAlert() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent alarmIntent = new Intent(this, MealbookersService.class);
        PendingIntent pending = PendingIntent.getService(this, 0, alarmIntent, 0);

        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + getCheckInterval() * 1000, pending);
    }

    /**
     * Gets the current wakeup interval.
     * @return
     */
    private int getCheckInterval() {
        Calendar c = Calendar.getInstance();
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        int hourOfDay = c.get(Calendar.HOUR_OF_DAY);
        int minutes = c.get(Calendar.MINUTE);
        int time = hourOfDay * 100 + minutes;
//        return 5;
        if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
//        17:15 -> 09:00 -> 1h
//        09:00 -> 09:45 -> 15min
//        09:45 -> 10:50 -> 7min
//        10:50 -> 12:50 -> 2min
//        12:50 -> 17:15 -> 15min
            if (time < 900) {
                return 60 * 60;
            } else if (time < 945) {
                return 60 * 15;
            } else if (time < 1050) {
                return 60 * 7;
            } else if (time < 1250) {
                return 60 * 2;
            } else if (time < 1750) {
                return 60 * 15;
            } else {
                return 60 * 60;
            }
        } else if (dayOfWeek == Calendar.SATURDAY) {
            if (time < 900) {
                return 60 * 60;
            } else if (time < 1400) {
                return 60 * 15;
            } else if (time < 1730) {
                return 60 * 30;
            } else {
                return 60 * 60;
            }
        // Sunday
        } else {
            return 60 * 60;
        }
    }

    /**
     * Get notification title from the pushNotification
     * @param pushNotification
     * @return
     */
    private String getTitle(PushNotification pushNotification) {
        // Get other user name
        String otherUserName;
        if (pushNotification.other_user != null)
            otherUserName = pushNotification.other_user.initials;
        else
            otherUserName = pushNotification.other_user_first_name;

        if (pushNotification.type == PushNotification.NOTIFICATION_TYPE_SUGGEST) {
            return pushNotification.suggestion.creator.initials + " wants to eat with you";
        }
        else if (pushNotification.type == PushNotification.NOTIFICATION_TYPE_ACCEPT) {
            return otherUserName + " has accepted your suggestion";
        }
        else if (pushNotification.type == PushNotification.NOTIFICATION_TYPE_CANCEL) {
            return otherUserName + " has canceled the suggestion";
        }
        else {
            return "You have been left alone";
        }
    }

    /**
     * Get notification text from the pushNotification
     * @param pushNotification
     * @return
     */
    private String getText(PushNotification pushNotification) {
        String timeString;
        if (pushNotification.suggestion != null) {
            timeString = " at " + pushNotification.suggestion.time;
        }
        else {
            timeString = " at " + pushNotification.suggestion_time_str;
        }
        String placeString;
        if (pushNotification.restaurant != null) {
            placeString = pushNotification.restaurant.name;
        }
        else {
            placeString = pushNotification.restaurant_name_str;
        }
        return placeString + timeString;
    }

    /**
     * Sends notification to android notification drawer.
     * @param pushNotification
     */
    private void sendNotification(PushNotification pushNotification) {

        String title = getTitle(pushNotification);
        String text = getText(pushNotification);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setAutoCancel(true);


        // Content action
        Intent contentIntent = new Intent(getApplicationContext(), MainActivity.class);
        contentIntent.putExtra("requestCode", MainActivity.REQUEST_CODE_NOTIFICATION_NOACCEPT);
        contentIntent.setAction(java.util.UUID.randomUUID().toString());
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                MainActivity.REQUEST_CODE_NOTIFICATION_NOACCEPT,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        mBuilder.setContentIntent(contentPendingIntent);

        if (pushNotification.type == PushNotification.NOTIFICATION_TYPE_SUGGEST) {
            // Accept action
            Intent acceptIntent = new Intent(getApplicationContext(), MainActivity.class);
            acceptIntent.putExtra("requestCode", MainActivity.REQUEST_CODE_NOTIFICATION_ACCEPT);
            acceptIntent.putExtra("token", pushNotification.token);
            PendingIntent acceptPendingIntent = PendingIntent.getActivity(
                    getApplicationContext(),
                    MainActivity.REQUEST_CODE_NOTIFICATION_ACCEPT,
                    acceptIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
            mBuilder.addAction(R.drawable.ic_done_grey600_24dp, getResources().getString(R.string.accept), acceptPendingIntent);
        }


        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(MainActivity.NOTIFICATION_ID, mBuilder.build());

        // Play ringtone
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();

            // Vibrate
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            long[] pattern = {0, 100, 300, 100, 500, 500};
            v.vibrate(pattern, -1);
        } catch (Exception e) {
            Log.e("sendNotification", "Playing ringtone failed", e);
        }
    }
}
