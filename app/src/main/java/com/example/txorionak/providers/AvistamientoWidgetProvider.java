package com.example.txorionak.providers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.widget.RemoteViews;

import com.example.txorionak.R;
import com.example.txorionak.activities.DashboardActivity;
import com.example.txorionak.activities.LoginActivity;
import com.example.txorionak.receivers.WidgetUpdateReceiver;
import com.example.txorionak.utils.AlarmUtils;


public class AvistamientoWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "AvistamientoWidgetProvider";
    public static final String UPDATE_ACTION = "com.example.txorionak.UPDATE_WIDGET";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // For each widget instance
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }

        // Schedule your periodic updates
        AlarmUtils.scheduleRepeatingAlarm(context);
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.avistamiento_widget);
        //views.setTextViewText(R.id.widget_text, "Cargando último avistamiento...");

        // Default intent
        Intent defaultIntent = new Intent(context, LoginActivity.class);
        PendingIntent defaultPendingIntent = PendingIntent.getActivity(
                context, appWidgetId, defaultIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Set refresh button intent
        Intent updateIntent = new Intent(context, WidgetUpdateReceiver.class);
        updateIntent.setAction(UPDATE_ACTION);
        context.sendBroadcast(updateIntent);

        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent updatePendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_refresh_button, updatePendingIntent);

        // Try to get user session data
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    UserSessionProvider.CONTENT_URI,
                    new String[]{UserSessionProvider.COLUMN_USER_ID, UserSessionProvider.COLUMN_USERNAME},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int userIdIndex = cursor.getColumnIndex(UserSessionProvider.COLUMN_USER_ID);
                int usernameIndex = cursor.getColumnIndex(UserSessionProvider.COLUMN_USERNAME);

                if (userIdIndex != -1 && usernameIndex != -1) {
                    int userId = cursor.getInt(userIdIndex);
                    String username = cursor.getString(usernameIndex);

                    // Set intent for widget click
                    Intent dashboardIntent = new Intent(context, DashboardActivity.class);
                    dashboardIntent.putExtra("usuario_id", userId);
                    dashboardIntent.putExtra("username", username);
                    dashboardIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    PendingIntent pendingIntent = PendingIntent.getActivity(
                            context, appWidgetId, dashboardIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
                } else {
                    // Set default click action if column indices are invalid
                    views.setOnClickPendingIntent(R.id.widget_layout, defaultPendingIntent);
                }
            } else {
                // No user data found, use default intent
                views.setOnClickPendingIntent(R.id.widget_layout, defaultPendingIntent);
            }
        } catch (Exception e) {
            Log.e("WidgetProvider", "Error getting user data", e);
            views.setOnClickPendingIntent(R.id.widget_layout, defaultPendingIntent);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        // Si recibimos la acción de actualización personalizada
        if (UPDATE_ACTION.equals(intent.getAction())) {
            // Disparar el receptor de actualización
            Intent updateIntent = new Intent(context, WidgetUpdateReceiver.class);
            context.sendBroadcast(updateIntent);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.d(TAG, "onEnabled called - setting up alarm");
        // Set up alarm when first widget is added
        AlarmUtils.scheduleRepeatingAlarm(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.d(TAG, "onDisabled called - canceling alarm");
        // Cancel alarm when last widget is removed
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, WidgetUpdateReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
    }
}
