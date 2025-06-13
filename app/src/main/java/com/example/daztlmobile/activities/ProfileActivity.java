package com.example.daztlmobile.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import com.example.daztlmobile.R;
import com.example.daztlmobile.utils.AuthUtils;

import daztl.DaztlServiceOuterClass;
import daztl.MusicServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class ProfileActivity extends AppCompatActivity {

    private TextView usernameTv, emailTv, fullNameTv;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        token = getIntent().getStringExtra("token");
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Token inválido o no recibido", Toast.LENGTH_LONG).show();
            finish();
            return;
        }


        usernameTv = findViewById(R.id.tvUsername);
        emailTv = findViewById(R.id.tvEmail);
        fullNameTv = findViewById(R.id.tvFullName);

        token = getIntent().getStringExtra("token");

        fetchUserProfile();
    }

    @SuppressLint("SetTextI18n")
    private void fetchUserProfile() {
        new Thread(() -> {
            try {
                ManagedChannel channel = ManagedChannelBuilder.forAddress("10.0.2.2", 50051)
                        .usePlaintext()
                        .build();
                MusicServiceGrpc.MusicServiceBlockingStub stub = MusicServiceGrpc.newBlockingStub(channel);

                DaztlServiceOuterClass.TokenRequest request = DaztlServiceOuterClass.TokenRequest.newBuilder()
                        .setToken(token)
                        .build();

                DaztlServiceOuterClass.UserProfileResponse response = null;

                try {
                    response = stub.getProfile(request);
                } catch (StatusRuntimeException e) {
                    if (e.getStatus().getCode() == Status.Code.UNAUTHENTICATED) {
                        String newAccessToken = AuthUtils.refreshAccessToken(ProfileActivity.this);
                        if (newAccessToken != null) {
                            token = newAccessToken;
                            request = DaztlServiceOuterClass.TokenRequest.newBuilder().setToken(token).build();
                            response = stub.getProfile(request); // reintento
                        } else {
                            runOnUiThread(() -> Toast.makeText(this, "Sesión expirada. Inicia sesión nuevamente.", Toast.LENGTH_LONG).show());
                            startActivity(new Intent(this, LoginActivity.class));
                            finish();
                            return;
                        }
                    } else {
                        throw e;
                    }
                }


                runOnUiThread(() -> {
                    usernameTv.setText(response.getUsername());
                    emailTv.setText(response.getEmail());
                    fullNameTv.setText(response.getFirstName() + " " + response.getLastName());
                });

                channel.shutdown();

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(ProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }
}
