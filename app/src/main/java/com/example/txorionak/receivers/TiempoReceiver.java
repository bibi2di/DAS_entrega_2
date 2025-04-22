package com.example.txorionak.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.txorionak.R;
import com.example.txorionak.activities.DashboardActivity;

public class TiempoReceiver extends BroadcastReceiver {
    private TextView textoTiempo;

    public TiempoReceiver() {
        // Required empty constructor
    }

    public TiempoReceiver(TextView textoTiempo) {
        this.textoTiempo = textoTiempo;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (textoTiempo != null) {
            String tiempo = intent.getStringExtra("tiempo");
            if (tiempo != null) {
                textoTiempo.setText("Tiempo de ruta: " + tiempo);
                textoTiempo.setVisibility(View.VISIBLE);
            }

            // Check if service was stopped
            if (intent.getBooleanExtra("service_stopped", false)) {
                // Find the activity to update button text
                if (context instanceof DashboardActivity) {
                    Button btnIniciarRuta = ((DashboardActivity) context).findViewById(R.id.btnIniciarRuta);
                    if (btnIniciarRuta != null) {
                        btnIniciarRuta.setText("Iniciar Ruta");
                    }
                    textoTiempo.setText("");
                    textoTiempo.setVisibility(View.GONE);
                }
            }
        }
    }

}