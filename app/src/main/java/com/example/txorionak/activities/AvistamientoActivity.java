package com.example.txorionak.activities;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.CalendarContract;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.txorionak.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AvistamientoActivity extends AppCompatActivity {

    private EditText etEspecie, etLatitud, etLongitud;
    private ImageView ivFoto;
    private Button btnTomarFoto, btnRegistrar;
    private Bitmap imagenSeleccionada;
    private final int CODIGO_FOTO = 1;
    private final int PERMISO_CAMARA = 100;
    private final int CODIGO_GALERIA = 2;
    private int usuario_id;
    private final int PERMISO_CALENDARIO = 102;

    private String locFoto;

    private final String URL_REGISTRO = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/bleon005/WEB/avistamiento.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avistamiento);
        usuario_id = getIntent().getIntExtra("usuario_id", -1);
        if (usuario_id == -1) {
            Toast.makeText(this, "Error: usuario no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etEspecie = findViewById(R.id.etEspecie);
        etLatitud = findViewById(R.id.etLatitud);
        etLongitud = findViewById(R.id.etLongitud);
        ivFoto = findViewById(R.id.ivFoto);
        btnTomarFoto = findViewById(R.id.btnTomarFoto);
        btnRegistrar = findViewById(R.id.btnRegistrar);

        btnTomarFoto.setOnClickListener(v -> {
            String[] opciones = {"Tomar foto", "Seleccionar desde galería"};
            new AlertDialog.Builder(this)
                    .setTitle("Selecciona una opción")
                    .setItems(opciones, (dialog, which) -> {
                        if (which == 0) {
                            // Tomar foto
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISO_CAMARA);
                            } else {
                                tomarFotoFileProvider();
                            }
                        } else {
                            // Galería
                            abrirGaleria();
                        }
                    })
                    .show();
        });


        btnRegistrar.setOnClickListener(v -> subirAvistamiento());
        obtenerUbicacion();
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, CODIGO_GALERIA);
    }

    private Uri crearUriImagen() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);

        // Save the file path for use with ACTION_VIEW intents
        locFoto = image.getAbsolutePath();

        return FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".fileprovider",
                image);
    }

    private void tomarFotoFileProvider() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Create the File where the photo should go
        Uri photoURI = null;
        try {
            photoURI = crearUriImagen();
        } catch (IOException ex) {
            // Error occurred while creating the File
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            return;
        }

        // Continue only if the File was successfully created
        if (photoURI != null) {
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            try {
                startActivityForResult(takePictureIntent, CODIGO_FOTO);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void obtenerUbicacion() {
        FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }
        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                etLatitud.setText(String.valueOf(location.getLatitude()));
                etLongitud.setText(String.valueOf(location.getLongitude()));
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISO_CAMARA && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            tomarFotoFileProvider();
        } else if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacion();
        } else if (requestCode == PERMISO_CALENDARIO && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Si se concedió el permiso, intentar nuevamente agregar al calendario
            String especie = etEspecie.getText().toString();
            String lat = etLatitud.getText().toString();
            String lng = etLongitud.getText().toString();
            agregarEventoCalendario(especie, lat, lng);
        } else {
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == CODIGO_FOTO) {
                // Handle camera photo - data may be null here with FileProvider
                if (locFoto != null) {
                    imagenSeleccionada = BitmapFactory.decodeFile(locFoto);

                    // Resize bitmap if needed to avoid memory issues
                    imagenSeleccionada = ajustarTamanoBitmap(imagenSeleccionada, 1024); // Max width/height 1024px

                    ivFoto.setImageBitmap(imagenSeleccionada);
                }
            } else if (requestCode == CODIGO_GALERIA && data != null) {
                Uri imageUri = data.getData();
                try {
                    imagenSeleccionada = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);

                    imagenSeleccionada = ajustarTamanoBitmap(imagenSeleccionada, 1024);

                    ivFoto.setImageBitmap(imagenSeleccionada);
                } catch (IOException e) {
                    Toast.makeText(this, "Error al cargar imagen", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // TAmaño de las imágenes:
    private Bitmap ajustarTamanoBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {

            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {

            height = maxSize;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }


    private void subirAvistamiento() {
        String especie = etEspecie.getText().toString();
        String lat = etLatitud.getText().toString();
        String lng = etLongitud.getText().toString();

        if (especie.isEmpty() || imagenSeleccionada == null) {
            Toast.makeText(this, "Completa todos los campos y toma una foto", Toast.LENGTH_SHORT).show();
            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        imagenSeleccionada.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        String imagenBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

        new Thread(() -> {
            try {
                URL url = new URL(URL_REGISTRO);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String datos = "usuario_id=" + URLEncoder.encode(String.valueOf(usuario_id), "UTF-8") +
                        "&especie=" + URLEncoder.encode(especie, "UTF-8") +
                        "&latitud=" + URLEncoder.encode(lat, "UTF-8") +
                        "&longitud=" + URLEncoder.encode(lng, "UTF-8") +
                        "&imagen=" + URLEncoder.encode(imagenBase64, "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(datos.getBytes());
                os.flush();
                os.close();

                // Read the response
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                final String serverResponse = response.toString();

                runOnUiThread(() -> {
                    Toast.makeText(this, serverResponse, Toast.LENGTH_SHORT).show();

                    // Only navigate back if successful
                    if (serverResponse.contains("correctamente")) {
                        agregarEventoCalendario(especie, lat, lng);
                        Intent intent = new Intent(this, DashboardActivity.class);
                        intent.putExtra("usuario_id", usuario_id);
                        intent.putExtra("username", getIntent().getStringExtra("username"));
                        startActivity(intent);
                        finish();
                    }
                });
            } catch (Exception e) {
                final String errorMsg = e.getMessage();
                runOnUiThread(() -> Toast.makeText(this, "Error: " + errorMsg, Toast.LENGTH_LONG).show());
                e.printStackTrace();
            }
        }).start();
    }

    private void agregarEventoCalendario(String especie, String latitud, String longitud) {

        long calendarId = obtenerIdCalendarioGoogle();
        // Verificar permiso
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR},
                    PERMISO_CALENDARIO);
            return;
        }

        // Crear evento en el calendario
        long startMillis = System.currentTimeMillis();
        long endMillis = startMillis + 3600000; // 1 hora después

        ContentResolver cr = getContentResolver();
        ContentValues values = new ContentValues();

        // Información del evento
        values.put(CalendarContract.Events.DTSTART, startMillis);
        values.put(CalendarContract.Events.DTEND, endMillis);
        values.put(CalendarContract.Events.TITLE, "Avistamiento: " + especie);
        values.put(CalendarContract.Events.DESCRIPTION,
                "Especie avistada: " + especie + "\nUbicación: " + latitud + ", " + longitud);
        values.put(CalendarContract.Events.EVENT_LOCATION, latitud + ", " + longitud);
        values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().getID());

        // Insertar evento
        try {
            Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);
            // Obtener el ID del evento
            long eventID = Long.parseLong(uri.getLastPathSegment());
            Toast.makeText(this, "Evento añadido al calendario", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error al añadir evento al calendario", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private long obtenerIdCalendarioGoogle() {
        String[] projection = new String[]{
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE
        };

        Cursor cursor = getContentResolver().query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                CalendarContract.Calendars.ACCOUNT_TYPE + " = ?",
                new String[]{"com.google"},
                null);

        if (cursor != null && cursor.moveToFirst()) {
            long calendarId = cursor.getLong(0);
            cursor.close();
            return calendarId;
        }

        return 1; // Valor predeterminado si no se encuentra un calendario de Google
    }


}
