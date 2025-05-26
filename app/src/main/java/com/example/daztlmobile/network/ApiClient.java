package com.example.daztlmobile.network;

import android.content.Context;
import com.example.daztlmobile.utils.SessionManager;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;

public class ApiClient {
    private static Retrofit retrofit = null;
    private static final String BASE_URL = "http://10.0.2.2:8000/api/";

    public static Retrofit getClient(Context ctx) {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request req = chain.request();
                            String token = new SessionManager(ctx).fetchToken();
                            if (token != null) {
                                req = req.newBuilder()
                                        .addHeader("Authorization", "Token " + token)
                                        .build();
                            }
                            return chain.proceed(req);
                        }
                    }).build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
