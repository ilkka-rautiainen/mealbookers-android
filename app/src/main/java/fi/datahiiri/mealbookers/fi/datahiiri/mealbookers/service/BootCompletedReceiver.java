package fi.datahiiri.mealbookers.fi.datahiiri.mealbookers.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

/**
 * Created by Lisa och Ilkka on 19.2.2015.
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    /**
     * Starts background service. This is called on BOOT_COMPLETED.
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent alarmIntent = new Intent(context, MealbookersService.class);
        PendingIntent pending = PendingIntent.getService(context, 0, alarmIntent, 0);

        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 10 * 1000, pending);
    }
}
