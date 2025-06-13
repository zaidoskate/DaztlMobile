package com.example.daztlmobile.utils;

import android.content.Context;

import com.example.daztlmobile.network.GrpcClient;

import daztl.DaztlServiceOuterClass;
import daztl.MusicServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;

public class AuthUtils {
    public static String refreshAccessToken(Context context) {
        SessionManager session = new SessionManager(context);
        String refreshToken = session.getRefreshToken();
        if (refreshToken == null) return null;

        try {
            ManagedChannel channel = GrpcClient.getChannel();
            MusicServiceGrpc.MusicServiceBlockingStub stub = MusicServiceGrpc.newBlockingStub(channel);

            DaztlServiceOuterClass.RefreshTokenRequest request = DaztlServiceOuterClass.RefreshTokenRequest
                    .newBuilder()
                    .setRefreshToken(refreshToken)
                    .build();

            DaztlServiceOuterClass.LoginResponse response = stub.refreshToken(request);

            session.saveTokens(response.getAccessToken(), response.getRefreshToken());

            return response.getAccessToken();
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            return null;
        }
    }
}
