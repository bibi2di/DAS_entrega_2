package com.example.txorionak.activities;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.txorionak.R;
import com.example.txorionak.providers.UserSessionProvider;

public class MainActivity extends AppCompatActivity {

    private Button btnLogin, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verificar sesión de usuario con el content provider
        if (checkUserSessionWithProvider()) {
            redirectToDashboard();
            return;
        }

        setContentView(R.layout.activity_main);

        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

    }

    private boolean checkUserSessionWithProvider() {
        Cursor cursor = getContentResolver().query(
                UserSessionProvider.CONTENT_URI,
                new String[]{UserSessionProvider.COLUMN_ID},
                null, null, null);

        boolean hasSession = false;
        if (cursor != null) {
            hasSession = cursor.getCount() > 0;
            cursor.close();
        }
        return hasSession;
    }

    private void redirectToDashboard() {
        Cursor cursor = getContentResolver().query(
                UserSessionProvider.CONTENT_URI,
                new String[]{UserSessionProvider.COLUMN_USER_ID, UserSessionProvider.COLUMN_USERNAME},
                null, null, null);

        int userId = -1;
        String username = "";

        if (cursor != null && cursor.moveToFirst()) {
            int userIdIndex = cursor.getColumnIndex(UserSessionProvider.COLUMN_USER_ID);
            int usernameIndex = cursor.getColumnIndex(UserSessionProvider.COLUMN_USERNAME);

            if (userIdIndex != -1 && usernameIndex != -1) {
                userId = cursor.getInt(userIdIndex);
                username = cursor.getString(usernameIndex);
            }
            cursor.close();
        }

        Intent intent = new Intent(this, DashboardActivity.class);
        intent.putExtra("usuario_id", userId);
        intent.putExtra("username", username);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        finish();
    }


}
