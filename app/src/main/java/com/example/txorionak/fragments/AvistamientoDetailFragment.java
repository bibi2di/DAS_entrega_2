package com.example.txorionak.fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.example.txorionak.R;

public class AvistamientoDetailFragment extends DialogFragment {
    private static final String ARG_ESPECIE = "especie";
    private static final String ARG_FECHA = "fecha";
    private static final String ARG_IMAGEN = "imagen";
    private static final String ARG_USUARIO_ID = "usuario_id";

    private String especie;
    private String fecha;
    private String imagen;
    private int usuarioId;

    // Interfaz para obtener información del usuario
    interface OnUsuarioInfoListener {
        void onUsuarioInfo(String nombreUsuario);
    }

    public static AvistamientoDetailFragment newInstance(String especie, String fecha, String imagen, int usuarioId) {
        AvistamientoDetailFragment fragment = new AvistamientoDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ESPECIE, especie);
        args.putString(ARG_FECHA, fecha);
        args.putString(ARG_IMAGEN, imagen);
        args.putInt(ARG_USUARIO_ID, usuarioId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            especie = getArguments().getString(ARG_ESPECIE);
            fecha = getArguments().getString(ARG_FECHA);
            imagen = getArguments().getString(ARG_IMAGEN);
            usuarioId = getArguments().getInt(ARG_USUARIO_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_avistamiento_detail, container, false);

        TextView tvEspecie = view.findViewById(R.id.tvEspecie);
        TextView tvFecha = view.findViewById(R.id.tvFecha);
        ImageView ivImagen = view.findViewById(R.id.ivImagen);
        Button btnCerrar = view.findViewById(R.id.btnCerrar);

        tvEspecie.setText(especie);
        tvFecha.setText("Avistado el: " + fecha);

        // Configurar el botón de cierre
        btnCerrar.setOnClickListener(v -> dismiss());


        // Cargar imagen
        if (imagen != null && !imagen.isEmpty()) {
            String imageUrlFull = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/bleon005/WEB/fotos/" + imagen;

            Glide.with(requireContext())
                    .load(imageUrlFull)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(ivImagen);

            ivImagen.setVisibility(View.VISIBLE);
        } else {
            ivImagen.setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Configurar el diálogo para que ocupe la mayor parte de la pantalla
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }


}