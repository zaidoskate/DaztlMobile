package com.example.daztlmobile.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.daztlmobile.R;
import com.example.daztlmobile.network.ApiClient;
import com.example.daztlmobile.network.ApiService;
import com.example.daztlmobile.network.GrpcClient;
import com.example.daztlmobile.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import daztl.MusicServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;


public class SignupActivity extends AppCompatActivity {
    private TextInputEditText etName, etUsername, etLastName, etEmail, etPassword, etConfirm;
    private Button btnSignup;
    private ApiService api;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager session = new SessionManager(this);
        if (session.fetchToken() != null) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_signup);

        api = ApiClient.getClient(this).create(ApiService.class);

        etName       = findViewById(R.id.etName);
        etUsername   = findViewById(R.id.etUsername);
        etLastName   = findViewById(R.id.etLastName);
        etEmail      = findViewById(R.id.etEmail);
        etPassword   = findViewById(R.id.etPassword);
        etConfirm    = findViewById(R.id.etConfirmPassword);
        btnSignup    = findViewById(R.id.btnSignup);

        btnSignup.setEnabled(false);

        TextWatcher passwordWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String pass = etPassword.getText().toString();
                String confirm = etConfirm.getText().toString();

                if (!confirm.equals(pass)) {
                    etConfirm.setError("Las contraseñas no coinciden");
                    btnSignup.setEnabled(false);
                } else {
                    etConfirm.setError(null);
                    btnSignup.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        etPassword.addTextChangedListener(passwordWatcher);
        etConfirm.addTextChangedListener(passwordWatcher);

        btnSignup.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String name = etName.getText().toString().trim();
            String lastName = etLastName.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty() || email.isEmpty() || name.isEmpty() || lastName.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            daztl.DaztlServiceOuterClass.RegisterRequest request = daztl.DaztlServiceOuterClass.RegisterRequest.newBuilder()
                    .setUsername(username)
                    .setPassword(password)
                    .setEmail(email)
                    .setFirstName(name)
                    .setLastName(lastName)
                    .build();

            ManagedChannel channel = GrpcClient.getChannel();
            MusicServiceGrpc.MusicServiceStub stub = MusicServiceGrpc.newStub(channel);

            stub.registerUser(request, new StreamObserver<daztl.DaztlServiceOuterClass.GenericResponse>() {
                @Override
                public void onNext(daztl.DaztlServiceOuterClass.GenericResponse response) {
                    runOnUiThread(() -> {
                        if (response.getStatus().equalsIgnoreCase("success")) {
                            Toast.makeText(SignupActivity.this, "Cuenta creada con éxito", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                            finish();
                        } else {
                            Toast.makeText(SignupActivity.this, response.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onError(Throwable t) {
                    runOnUiThread(() ->
                            Toast.makeText(SignupActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show()
                    );
                }

                @Override
                public void onCompleted() {
                    channel.shutdown();
                }
            });
        });



        findViewById(R.id.tvGoToLogin).setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }
}
