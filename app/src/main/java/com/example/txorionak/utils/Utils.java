package com.example.txorionak.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class Utils {

    public static String getCityFromLocation(Context context, double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getLocality(); // Esto da la ciudad
                if (city != null) {
                    return city;
                } else {
                    // Si no encuentra ciudad, intenta con admin area o subadmin
                    return address.getSubAdminArea() != null ? address.getSubAdminArea() : address.getAdminArea();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Ubicación desconocida";
    }
}
