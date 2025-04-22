package com.example.txorionak.activities;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.txorionak.R;
import com.example.txorionak.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class HistorialActivity extends AppCompatActivity {

    private RecyclerView recyclerAvistamientos;
    private AvistamientoAdapter adaptador;
    private final ArrayList<Avistamiento> avistamientos = new ArrayList<>();
    private int usuario_id;

    private final String URL_HISTORIAL = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/bleon005/WEB/obtener_avistamientos.php";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial);

        // Get usuario_id from intent
        usuario_id = getIntent().getIntExtra("usuario_id", -1);
        if (usuario_id == -1) {
            Toast.makeText(this, "Error: usuario no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup RecyclerView
        recyclerAvistamientos = findViewById(R.id.rvHistorial);
        recyclerAvistamientos.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new AvistamientoAdapter(this, avistamientos);
        recyclerAvistamientos.setAdapter(adaptador);

        // Add back button
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());

        // Load data
        cargarAvistamientos();
    }

    private void cargarAvistamientos() {
        TextView tvEmptyState = findViewById(R.id.tvEmptyState);
        new Thread(() -> {
            try {
                // Build URL with user_id parameter
                URL url = new URL(URL_HISTORIAL + "?usuario_id=" + usuario_id);
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

                // Parse JSON
                JSONArray jsonArray = new JSONArray(response.toString());
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);

                    Avistamiento avistamiento = new Avistamiento();
                    avistamiento.setId(jsonObject.getInt("id"));
                    avistamiento.setEspecie(jsonObject.getString("especie"));
                    avistamiento.setLatitud(jsonObject.getDouble("latitud"));
                    avistamiento.setLongitud(jsonObject.getDouble("longitud"));
                    avistamiento.setImagen(jsonObject.getString("imagen"));
                    avistamiento.setFecha(jsonObject.getString("fecha_formateada"));

                    avistamientos.add(avistamiento);
                }

                runOnUiThread(() -> {
                    if (avistamientos.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        recyclerAvistamientos.setVisibility(View.GONE);
                    } else {
                        tvEmptyState.setVisibility(View.GONE);
                        recyclerAvistamientos.setVisibility(View.VISIBLE);
                        adaptador.notifyDataSetChanged();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error al cargar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}

// Model class for Avistamiento
class Avistamiento {
    private int id;
    private String especie;
    private double latitud;
    private double longitud;
    private String imagen;
    private String fecha;

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEspecie() { return especie; }
    public void setEspecie(String especie) { this.especie = especie; }

    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }

    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }

    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    @Override
    public String toString() {
        return fecha + " - " + especie;
    }
}

// RecyclerView Adapter
class AvistamientoAdapter extends RecyclerView.Adapter<AvistamientoAdapter.ViewHolder> {

    private final List<Avistamiento> avistamientos;
    private final Context context;
    private final String URL_BASE_IMAGENES = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/bleon005/WEB/fotos/";

    public AvistamientoAdapter(Context context, List<Avistamiento> avistamientos) {
        this.context = context;
        this.avistamientos = avistamientos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_avistamiento, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Avistamiento avistamiento = avistamientos.get(position);
        holder.tvEspecie.setText(avistamiento.getEspecie());
        holder.tvFecha.setText(avistamiento.getFecha());

        String city = Utils.getCityFromLocation(context, avistamiento.getLatitud(), avistamiento.getLongitud());
        holder.tvCoordenadas.setText("Lugar: " + city);

        if (avistamiento.getImagen() != null && !avistamiento.getImagen().isEmpty()) {
            String urlImagen = URL_BASE_IMAGENES + avistamiento.getImagen();
            Glide.with(context)
                    .load(urlImagen)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .centerCrop()
                    .into(holder.ivImagen);
            holder.ivImagen.setVisibility(View.VISIBLE);
        } else {
            holder.ivImagen.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return avistamientos.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEspecie;
        TextView tvFecha;
        TextView tvCoordenadas;
        ImageView ivImagen;

        ViewHolder(View itemView) {
            super(itemView);
            tvEspecie = itemView.findViewById(R.id.tvEspecie);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvCoordenadas = itemView.findViewById(R.id.tvCoordenadas);
            ivImagen = itemView.findViewById(R.id.ivImagen);
        }
    }
}

