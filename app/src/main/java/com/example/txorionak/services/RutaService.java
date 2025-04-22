package com.example.txorionak.services;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;


import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.txorionak.activities.DashboardActivity;

import java.util.Locale;

public class RutaService extends Service {

    // Change this to match DashboardActivity
    public static final String ACTION_UPDATE_TIEMPO = "com.example.txorionak.UPDATE_TIEMPO";
    public static final String ACTION_STOP_SERVICE = "com.example.txorionak.STOP_RUTA_SERVICE";

    private Handler handler;
    private Runnable tiempoRunnable;
    private long tiempoInicio;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        // Ensure notification channel exists
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                "avistamiento_channel",
                "Rutas de Avistamiento",
                NotificationManager.IMPORTANCE_LOW); // Using LOW for a timer is better
        channel.setDescription("Canal para el servicio de rutas");

        notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP_SERVICE.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        tiempoInicio = SystemClock.elapsedRealtime();
        startForeground(1, createNotification("00:00:00"));

        handler = new Handler(Looper.getMainLooper());
        tiempoRunnable = new Runnable() {
            @Override
            public void run() {
                long tiempoActual = SystemClock.elapsedRealtime() - tiempoInicio;
                String tiempoFormateado = formatTime(tiempoActual);

                // Update notification
                notificationManager.notify(1, createNotification(tiempoFormateado));

                // Send broadcast
                Intent i = new Intent(ACTION_UPDATE_TIEMPO);
                i.putExtra("tiempo", tiempoFormateado);
                sendBroadcast(i);

                handler.postDelayed(this, 1000);
            }
        };
        handler.post(tiempoRunnable);

        return START_STICKY;
    }

    private Notification createNotification(String tiempo) {
        Intent stopIntent = new Intent(this, RutaService.class);
        stopIntent.setAction(ACTION_STOP_SERVICE);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Create pending intent for opening the app when notification is clicked
        Intent mainIntent = new Intent(this, DashboardActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, "avistamiento_channel")
                .setContentTitle("Ruta en curso")
                .setContentText("Tiempo: " + tiempo)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_delete, "Detener", stopPendingIntent)
                .setOngoing(true)
                .build();
    }

    private String formatTime(long millis) {
        long segundos = millis / 1000;
        long min = (segundos / 60) % 60;
        long hr = segundos / 3600;
        long sec = segundos % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hr, min, sec);
    }

    @Override
    public void onDestroy() {
        if (handler != null && tiempoRunnable != null) {
            handler.removeCallbacks(tiempoRunnable);
        }

        // Send broadcast that service was stopped
        Intent stopBroadcast = new Intent(ACTION_UPDATE_TIEMPO);
        stopBroadcast.putExtra("service_stopped", true);
        sendBroadcast(stopBroadcast);

        stopForeground(true);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}