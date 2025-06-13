package com.example.daztlmobile.network;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GrpcClient {
    private static ManagedChannel channel;

    public static ManagedChannel getChannel() {
        if (channel == null || channel.isShutdown() || channel.isTerminated()) {
            channel = ManagedChannelBuilder
                    .forAddress("10.0.2.2", 50051)
                    .usePlaintext()
                    .build();
        }
        return channel;
    }

    public static void shutdownChannel() {
        if (channel != null && !channel.isShutdown()) {
            try {
                channel.shutdown().awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                channel.shutdownNow();
            }
        }
        channel = null;
    }
}
