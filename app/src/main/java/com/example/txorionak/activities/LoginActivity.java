package com.example.txorionak.activities;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.txorionak.R;
import com.example.txorionak.providers.UserSessionProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class LoginActivity extends AppCompatActivity {
    EditText email, password;
    Button login, toRegister;
    CheckBox rememberMe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email = findViewById(R.id.editEmail);
        password = findViewById(R.id.editPassword);
        login = findViewById(R.id.btnLogin);
        toRegister = findViewById(R.id.btnToRegister);
        rememberMe = findViewById(R.id.checkBoxRememberMe);

        // Check if we have stored credentials with "Remember Me"
        checkSavedCredentials();

        login.setOnClickListener(v -> {
            new LoginTask().execute(email.getText().toString(), password.getText().toString());
        });

        toRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void checkSavedCredentials() {
        Cursor cursor = getContentResolver().query(
                UserSessionProvider.CONTENT_URI,
                new String[]{
                        UserSessionProvider.COLUMN_EMAIL,
                        UserSessionProvider.COLUMN_PASSWORD,
                        UserSessionProvider.COLUMN_REMEMBER_ME
                },
                UserSessionProvider.COLUMN_REMEMBER_ME + "=?",
                new String[]{"1"},
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            int emailIndex = cursor.getColumnIndex(UserSessionProvider.COLUMN_EMAIL);
            int passwordIndex = cursor.getColumnIndex(UserSessionProvider.COLUMN_PASSWORD);

            if (emailIndex != -1 && passwordIndex != -1) {
                String savedEmail = cursor.getString(emailIndex);
                String savedPassword = cursor.getString(passwordIndex);

                if (savedEmail != null && !savedEmail.isEmpty()) {
                    email.setText(savedEmail);
                }

                if (savedPassword != null && !savedPassword.isEmpty()) {
                    password.setText(savedPassword);
                }

                rememberMe.setChecked(true);
            }
            cursor.close();
        }
    }

    class LoginTask extends AsyncTask<String, Void, JSONObject> {
        protected JSONObject doInBackground(String... params) {
            try {
                URL url = new URL("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/bleon005/WEB/login.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData = "email=" + URLEncoder.encode(params[0], "UTF-8") +
                        "&password=" + URLEncoder.encode(params[1], "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    return new JSONObject(result.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(JSONObject response) {
            try {
                if (response != null && response.getBoolean("success")) {
                    JSONObject user = response.getJSONObject("user");
                    int usuarioId = user.getInt("id");
                    String username = user.getString("nombre");

                    // Save user session with ContentProvider
                    saveUserSessionWithProvider(usuarioId, username, email.getText().toString(),
                            password.getText().toString(), rememberMe.isChecked());

                    Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                    intent.putExtra("usuario_id", usuarioId);
                    intent.putExtra("username", username);
                    // Use flags to clear activity stack
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Login fallido", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                Toast.makeText(LoginActivity.this, "Error de datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    private void saveUserSessionWithProvider(int userId, String username, String userEmail,
                                             String userPassword, boolean remember) {
        // First, clear any existing sessions
        getContentResolver().delete(UserSessionProvider.CONTENT_URI, null, null);

        // Create new content values
        ContentValues values = new ContentValues();
        values.put(UserSessionProvider.COLUMN_USER_ID, userId);
        values.put(UserSessionProvider.COLUMN_USERNAME, username);
        values.put(UserSessionProvider.COLUMN_REMEMBER_ME, remember ? 1 : 0);

        // Only store email and password if remember me is checked
        if (remember) {
            values.put(UserSessionProvider.COLUMN_EMAIL, userEmail);
            values.put(UserSessionProvider.COLUMN_PASSWORD, userPassword);
        }

        // Insert the session
        getContentResolver().insert(UserSessionProvider.CONTENT_URI, values);
    }
}