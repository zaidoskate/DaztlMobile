package com.example.daztlmobile.network;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GrpcClient {
    private static ManagedChannel channel;

    public static ManagedChannel getChannel() {
        if (channel == null) {
            channel = ManagedChannelBuilder
                    .forAddress("10.0.2.2", 50051)
                    .usePlaintext()
                    .build();
        }
        return channel;
    }
}
