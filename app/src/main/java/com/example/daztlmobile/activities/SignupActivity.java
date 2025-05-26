package com.example.daztlmobile.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.daztlmobile.R;
import com.example.daztlmobile.models.RegisterRequest;
import com.example.daztlmobile.models.UserResponse;
import com.example.daztlmobile.network.ApiClient;
import com.example.daztlmobile.network.ApiService;
import com.google.android.material.textfield.TextInputEditText;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {
    private TextInputEditText etName, etEmail, etPassword, etConfirm;
    private Button btnSignup;
    private ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        api = ApiClient.getClient(this).create(ApiService.class);

        etName     = findViewById(R.id.etName);
        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirm  = findViewById(R.id.etConfirmPassword);
        btnSignup  = findViewById(R.id.btnSignup);

        btnSignup.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString();
            String confirm = etConfirm.getText().toString();

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || !pass.equals(confirm)) {
                Toast.makeText(this, "Completa todos los campos correctamente", Toast.LENGTH_SHORT).show();
                return;
            }

            RegisterRequest req = new RegisterRequest();
            req.username = name;
            req.email = email;
            req.password = pass;
            req.first_name = name;
            req.last_name = "";

            api.register(req).enqueue(new Callback<UserResponse>() {
                @Override
                public void onResponse(Call<UserResponse> call, Response<UserResponse> res) {
                    if (res.isSuccessful()) {
                        Toast.makeText(SignupActivity.this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(SignupActivity.this, "Error en registro", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<UserResponse> call, Throwable t) {
                    Toast.makeText(SignupActivity.this, "Fallo de red", Toast.LENGTH_SHORT).show();
                }
            });
        });

        findViewById(R.id.tvGoToLogin).setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }
}
