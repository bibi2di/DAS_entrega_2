package com.example.txorionak.activities;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.Manifest;
import android.widget.Toast;
import android.database.Cursor;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.txorionak.fragments.AvistamientoDetailFragment;
import com.example.txorionak.R;
import com.example.txorionak.services.RutaService;
import com.example.txorionak.receivers.TiempoReceiver;
import com.example.txorionak.providers.UserSessionProvider;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import android.content.IntentFilter;
import android.content.Context;


public class DashboardActivity extends AppCompatActivity {

    private MapView map;
    private int usuarioId;
    private String username;
    private final String URL_AVISTAMIENTOS = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/bleon005/WEB/obtener_avistamientos.php";
    public static final String UPDATE_TIEMPO = "com.example.txorionak.UPDATE_TIEMPO";
    private TiempoReceiver tiempoReceiver;
    private Button btnIniciarRuta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        setContentView(R.layout.activity_dashboard);

        // Get user info from intent
        usuarioId = getIntent().getIntExtra("usuario_id", -1);
        username = getIntent().getStringExtra("username");
        btnIniciarRuta = findViewById(R.id.btnIniciarRuta);

        if (username != null) {
            TextView tvWelcome = findViewById(R.id.tvWelcome);
            tvWelcome.setText("Bienvenido, " + username + " 🐦");
        }

        // Initialize map
        map = new MapView(this);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        FrameLayout mapContainer = findViewById(R.id.mapContainer);
        mapContainer.addView(map);

        map.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                // Cerrar cualquier InfoWindow abierta al tocar en el mapa
                InfoWindow.closeAllInfoWindowsOn(map);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        }));

        // Set up navigation buttons
        findViewById(R.id.btnRegisterSighting).setOnClickListener(v -> {
            Intent intent = new Intent(this, AvistamientoActivity.class);
            intent.putExtra("username", username);
            intent.putExtra("usuario_id", usuarioId);
            startActivity(intent);
        });

        findViewById(R.id.btnHistory).setOnClickListener(v -> {
            Intent intent = new Intent(this, HistorialActivity.class);
            intent.putExtra("username", username);
            intent.putExtra("usuario_id", usuarioId);
            startActivity(intent);
        });

        btnIniciarRuta.setOnClickListener(v -> {
            // Check if service is already running
            if (isServiceRunning(RutaService.class)) {
                // Stop the service
                Intent stopIntent = new Intent(this, RutaService.class);
                stopIntent.setAction(RutaService.ACTION_STOP_SERVICE);
                ContextCompat.startForegroundService(this, stopIntent);
                btnIniciarRuta.setText("Iniciar Ruta");
            } else {
                // Check permission and start service
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
                        return;
                    }
                }

                // Start the service
                Intent intent = new Intent(this, RutaService.class);
                ContextCompat.startForegroundService(this, intent);
                btnIniciarRuta.setText("Detener Ruta");
            }
        });


        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            logout();
        });

        NotificationChannel channel = new NotificationChannel(
                "avistamiento_channel",
                "Avistamientos FCM",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Notificaciones de avistamientos remotos");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);

        TextView textoTiempo = findViewById(R.id.textoTiempo);
        tiempoReceiver = new TiempoReceiver(textoTiempo);

        if (isServiceRunning(RutaService.class)) {
            btnIniciarRuta.setText("Detener Ruta");
        } else {
            btnIniciarRuta.setText("Iniciar Ruta");
            textoTiempo.setText("");
            textoTiempo.setVisibility(View.GONE);
        }

        // Get user location and load avistamientos
        getUserLocation();
        cargarAvistamientosEnMapa();
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                return;
            }

            // Get new FCM registration token
            String token = task.getResult();
            Log.d("FCM", "Token: " + token);
        });
    }

    private boolean isServiceRunning(Class<RutaService> rutaServiceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (RutaService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void getUserLocation() {
        FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }

        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                GeoPoint userPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                IMapController controller = map.getController();
                controller.setZoom(14.0);
                controller.setCenter(userPoint);

                // Add user location marker
                Marker marker = new Marker(map);
                marker.setPosition(userPoint);
                marker.setTitle("Tu ubicación");
                marker.setSnippet("Estás aquí");
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marker.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_mylocation));
                map.getOverlays().add(marker);
            }
        });
    }

    private void cargarAvistamientosEnMapa() {
        new Thread(() -> {
            try {
                // Get all avistamientos (without user_id filter)
                URL url = new URL(URL_AVISTAMIENTOS);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                // Read JSON response
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse JSON and add markers on the map
                JSONArray jsonArray = new JSONArray(response.toString());
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject avistamiento = jsonArray.getJSONObject(i);
                    final double latitud = avistamiento.getDouble("latitud");
                    final double longitud = avistamiento.getDouble("longitud");
                    final String especie = avistamiento.getString("especie");
                    final String fecha = avistamiento.getString("fecha_formateada");
                    final String imagen = avistamiento.getString("imagen");

                    // Obtener el ID de usuario si está disponible
                    final int usuarioAvistamiento;
                    if (avistamiento.has("usuario_id")) {
                        usuarioAvistamiento = avistamiento.getInt("usuario_id");
                    } else {
                        usuarioAvistamiento = -1; // Valor por defecto si no está disponible
                    }

                    // Update UI on main thread
                    runOnUiThread(() -> {
                        // Create marker for this avistamiento
                        GeoPoint point = new GeoPoint(latitud, longitud);
                        Marker marker = new Marker(map);
                        marker.setPosition(point);
                        marker.setTitle(especie);
                        marker.setSnippet("Avistado el: " + fecha);
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        marker.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_camera));

                        // Configurar la acción al hacer clic en el marcador
                        marker.setOnMarkerClickListener((m, mapView) -> {
                            // Mostrar el fragment con la información del avistamiento
                            AvistamientoDetailFragment fragment = AvistamientoDetailFragment.newInstance(
                                    especie, fecha, imagen, usuarioAvistamiento);
                            fragment.show(getSupportFragmentManager(), "avistamiento_detail");
                            return true;
                        });

                        map.getOverlays().add(marker);
                    });
                }

                // Refresh map
                runOnUiThread(() -> map.invalidate());

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(DashboardActivity.this,
                        "Error al cargar avistamientos: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    private void logout() {
        // Clear user session data with ContentProvider
        getContentResolver().delete(UserSessionProvider.CONTENT_URI, null, null);

        // Return to MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finishAffinity(); // Close all activities in the stack
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocation();
            } else {
                Toast.makeText(this, "Se requiere permiso de ubicación para mostrar el mapa correctamente",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void checkSession() {
        Cursor cursor = getContentResolver().query(
                UserSessionProvider.CONTENT_URI,
                new String[]{UserSessionProvider.COLUMN_USER_ID},
                null, null, null);

        boolean hasSession = false;
        if (cursor != null) {
            hasSession = ((android.database.Cursor) cursor).getCount() > 0;
            ((android.database.Cursor) cursor).close();
        }

        if (!hasSession) {
            // Session expired or was deleted, return to login
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        checkSession();
        cargarAvistamientosEnMapa();

        // Register BroadcastReceiver
        IntentFilter filter = new IntentFilter(UPDATE_TIEMPO);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(tiempoReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(tiempoReceiver, filter);
        }

        // Update UI based on service status
        if (isServiceRunning(RutaService.class)) {
            btnIniciarRuta.setText("Detener Ruta");
            TextView textoTiempo = findViewById(R.id.textoTiempo);
            textoTiempo.setVisibility(View.VISIBLE);
        } else {
            btnIniciarRuta.setText("Iniciar Ruta");
            TextView textoTiempo = findViewById(R.id.textoTiempo);
            textoTiempo.setText("");
            textoTiempo.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
        try {
            unregisterReceiver(tiempoReceiver);
        } catch (IllegalArgumentException e) {

        }
    }


}

