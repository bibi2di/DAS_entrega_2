package com.example.txorionak.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.example.txorionak.providers.AvistamientoWidgetProvider;

public class AlarmUtils {
    public static void scheduleRepeatingAlarm(Context context) {
        Intent intent = new Intent(context, AvistamientoWidgetProvider.class);
        intent.setAction(AvistamientoWidgetProvider.UPDATE_ACTION);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setRepeating(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime(),
                    15 * 1000, // Actualización cada 15 segundos
                    pendingIntent
            );
        }
    }
}

