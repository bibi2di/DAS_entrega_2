package com.example.txorionak.receivers;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;

import com.example.txorionak.providers.AvistamientoWidgetProvider;
import com.example.txorionak.R;
import com.example.txorionak.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class WidgetUpdateReceiver extends BroadcastReceiver {
    private static final String API_URL = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/bleon005/WEB/obtener_ultimo.php";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Fetch data in a background thread
        new Thread(() -> {
            try {
                // Get the latest sighting from your server
                JSONObject latestSighting = fetchLatestSighting();

                if (latestSighting != null) {
                    String especie = latestSighting.getString("especie");
                    String fechaFormateada = latestSighting.getString("fecha_formateada");
                    double lat = latestSighting.getDouble("latitud");
                    double longitud = latestSighting.getDouble("longitud");

                    String ciudad = Utils.getCityFromLocation(context, lat, longitud);

                    // Update the widget on the main thread
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(() -> updateWidgetUI(context, especie, fechaFormateada, ciudad));
                } else {
                    // Handle the case when no sightings are available
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(() -> updateWidgetUI(context, "No hay avistamientos", "", ""));
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Update widget with error message
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() -> updateWidgetUI(context, "Error al cargar avistamientos: " + e.getMessage(), "", ""));
            }
        }).start();
    }

    private void updateWidgetUI(Context context, String especie, String fecha, String ciudad) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.avistamiento_widget);

        if (fecha.isEmpty()) {
            views.setTextViewText(R.id.widget_text, especie);
        } else {
            views.setTextViewText(R.id.widget_text, especie + " avistado el " + fecha + " en " + ciudad);
        }

        // Obtener el AppWidgetManager y actualizar el widget
        ComponentName widget = new ComponentName(context, AvistamientoWidgetProvider.class);
        AppWidgetManager.getInstance(context).updateAppWidget(widget, views);
    }

    private JSONObject fetchLatestSighting() throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String responseStr = response.toString();
                Log.d("WidgetUpdateReceiver", "Respuesta cruda: " + responseStr);

                // Check if the response is a direct JSON object or an array
                if (responseStr.trim().startsWith("{")) {
                    // It's a direct JSON object
                    return new JSONObject(responseStr);
                } else {
                    // It's likely an array
                    JSONArray avistamientos = new JSONArray(responseStr);
                    if (avistamientos.length() > 0) {
                        // Return the first item (most recent sighting)
                        return avistamientos.getJSONObject(0);
                    }
                }
            }
        } finally {
            connection.disconnect();
        }

        return null;
    }
}
