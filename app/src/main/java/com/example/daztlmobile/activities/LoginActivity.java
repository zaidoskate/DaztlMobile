package com.example.daztlmobile.activities;

import android.os.Bundle;
import android.content.Intent;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.daztlmobile.R;
import com.example.daztlmobile.network.GrpcClient;
import com.example.daztlmobile.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import daztl.DaztlServiceOuterClass;
import daztl.MusicServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText etUsername, etPassword;
    private Button btnLogin;
    private SessionManager session;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        session = new SessionManager(this);

        etUsername = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String email = etUsername.getText().toString().trim();
            String pass = etPassword.getText().toString();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Completa ambos campos", Toast.LENGTH_SHORT).show();
                return;
            }

            executor.execute(() -> {
                try {
                    ManagedChannel channel = GrpcClient.getChannel();

                    MusicServiceGrpc.MusicServiceBlockingStub stub = MusicServiceGrpc.newBlockingStub(channel);

                    DaztlServiceOuterClass.LoginRequest request = DaztlServiceOuterClass.LoginRequest.newBuilder()
                            .setUsername(email)
                            .setPassword(pass)
                            .build();

                    daztl.DaztlServiceOuterClass.LoginResponse response = stub.loginUser(request);

                    runOnUiThread(() -> {
                        session.saveToken(response.getAccessToken());
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        startActivity(intent);
                        channel.shutdown();
                    });


                } catch (StatusRuntimeException e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(LoginActivity.this, "Credenciales inválidas", Toast.LENGTH_SHORT).show()
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(LoginActivity.this, "Error de red o del servidor", Toast.LENGTH_SHORT).show()
                    );
                }
            });
        });
    }
}
