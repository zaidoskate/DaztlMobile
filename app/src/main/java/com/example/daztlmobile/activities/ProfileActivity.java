package com.example.daztlmobile.activities;

import android.net.Uri;
import android.util.Base64;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import com.bumptech.glide.Glide;
import com.example.daztlmobile.R;
import com.example.daztlmobile.utils.AuthUtils;
import com.example.daztlmobile.utils.FileUtils;
import com.example.daztlmobile.utils.SessionManager;

import java.io.File;
import java.io.IOException;

import daztl.DaztlServiceOuterClass;
import daztl.MusicServiceGrpc;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName,etUsername, etEmail, etPassword;
    private ImageView imgProfilePicture;

    private Button btnSaveChanges;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;


    private String token;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        sessionManager = new SessionManager(this);
        token = getIntent().getStringExtra("token");

        if (token == null || token.isEmpty()) {
            token = sessionManager.fetchToken();
        }

        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Token inválido o no recibido", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        imgProfilePicture = findViewById(R.id.imgProfilePicture);
        imgProfilePicture.setOnClickListener(v -> openImageChooser());


        btnSaveChanges.setOnClickListener(v -> updateUserProfile());

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);


        if (isTokenValid(token)) {
            fetchUserProfile();
        } else {
            new Thread(() -> {
                String newToken = AuthUtils.refreshAccessToken(ProfileActivity.this);
                if (newToken != null && isTokenValid(newToken)) {
                    token = newToken;
                    sessionManager.saveToken(token);
                    runOnUiThread(this::fetchUserProfile);
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(ProfileActivity.this, "Sesión expirada. Inicia sesión nuevamente.", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                        finish();
                    });
                }
            }).start();
        }
    }

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Selecciona una imagen"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            imgProfilePicture.setImageURI(imageUri);
        }
    }


    private void updateUserProfile() {
        new Thread(() -> {
            try {
                ManagedChannel channel = ManagedChannelBuilder.forAddress("10.0.2.2", 50051)
                        .usePlaintext()
                        .build();

                MusicServiceGrpc.MusicServiceBlockingStub stub = MusicServiceGrpc.newBlockingStub(channel);

                Metadata metadata = new Metadata();
                Metadata.Key<String> AUTHORIZATION_HEADER = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
                metadata.put(AUTHORIZATION_HEADER, "Bearer " + token);
                ClientInterceptor interceptor = MetadataUtils.newAttachHeadersInterceptor(metadata);
                stub = stub.withInterceptors(interceptor);

                String newUsername = etUsername.getText().toString().trim();
                String newEmail = etEmail.getText().toString().trim();
                String newPassword = etPassword.getText().toString().trim();
                String newFirstName = etFirstName.getText().toString().trim();
                String newLastName = etLastName.getText().toString().trim();

                DaztlServiceOuterClass.UpdateProfileRequest.Builder requestBuilder =
                DaztlServiceOuterClass.UpdateProfileRequest.newBuilder()
                                .setToken(token)
                                .setUsername(newUsername)
                                .setPassword(newPassword)
                                .setEmail(newEmail)
                                .setFirstName(newFirstName)
                                .setLastName(newLastName);

                    DaztlServiceOuterClass.GenericResponse response = stub.updateProfile(requestBuilder.build());
                    runOnUiThread(() -> Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show());
                if (imageUri != null) {
                    uploadProfilePicture(imageUri);
                }

                channel.shutdown();

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void uploadProfilePicture(Uri imageUri) {
        try {
            String filePath = FileUtils.getPath(this, imageUri);
            File file = new File(filePath);
            RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(imageUri)), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("profile_picture", file.getName(), requestFile);
            RequestBody tokenPart = RequestBody.create(MediaType.parse("text/plain"), "Bearer " + token);

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("http://10.0.2.2:8000/api/profile/upload_picture/")
                    .addHeader("Authorization", "Bearer " + token)
                    .post(new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("profile_picture", file.getName(), requestFile)
                            .build())
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Error al subir imagen", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(ProfileActivity.this, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ProfileActivity.this, "Error al actualizar imagen", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show();
        }
    }


    @SuppressLint("SetTextI18n")
    private void fetchUserProfile() {
        new Thread(() -> {
            try {
                ManagedChannel channel = ManagedChannelBuilder.forAddress("10.0.2.2", 50051)
                        .usePlaintext()
                        .build();

                MusicServiceGrpc.MusicServiceBlockingStub stub = MusicServiceGrpc.newBlockingStub(channel);

                Metadata metadata = new Metadata();
                Metadata.Key<String> AUTHORIZATION_HEADER = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
                metadata.put(AUTHORIZATION_HEADER, "Bearer " + token);

                ClientInterceptor interceptor = MetadataUtils.newAttachHeadersInterceptor(metadata);
                stub = MusicServiceGrpc.newBlockingStub(channel).withInterceptors(interceptor);

                DaztlServiceOuterClass.UserProfileResponse response;

                try {
                    response = stub.getProfile(DaztlServiceOuterClass.Empty.newBuilder().build());

                } catch (StatusRuntimeException e) {
                    if (e.getStatus().getCode() == Status.Code.UNAUTHENTICATED) {
                        String newAccessToken = AuthUtils.refreshAccessToken(ProfileActivity.this);
                        if (newAccessToken != null) {
                            token = newAccessToken;
                            sessionManager.saveToken(token);

                            metadata.put(AUTHORIZATION_HEADER, "Bearer " + token);
                            ClientInterceptor interceptor1 = MetadataUtils.newAttachHeadersInterceptor(metadata);
                            stub = MusicServiceGrpc.newBlockingStub(channel).withInterceptors(interceptor1);
                            response = stub.getProfile(DaztlServiceOuterClass.Empty.newBuilder().build());
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

                DaztlServiceOuterClass.UserProfileResponse finalResponse = response;
                runOnUiThread(() -> {
                    etUsername.setText(finalResponse.getUsername());
                    etEmail.setText(finalResponse.getEmail());
                    etFirstName.setText(finalResponse.getFirstName());
                    etLastName.setText(finalResponse.getLastName());

                    String profileImageUrl = finalResponse.getProfileImageUrl();
                    profileImageUrl = profileImageUrl.replace("localhost", "10.0.2.2");
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_default_profile)
                                .error(R.drawable.ic_default_profile)
                                .into(imgProfilePicture);
                    } else {
                        imgProfilePicture.setImageResource(R.drawable.ic_default_profile);
                    }
                });

                channel.shutdown();

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(ProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }
    public boolean isTokenValid(String token) {
        if (token == null || token.isEmpty()) return false;

        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;

            // Payload base64url sin padding
            String payloadJson = new String(Base64.decode(parts[1], Base64.URL_SAFE), "UTF-8");
            JSONObject payload = new JSONObject(payloadJson);

            long exp = payload.optLong("exp", 0);
            long now = System.currentTimeMillis() / 1000;

            return exp > now;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
