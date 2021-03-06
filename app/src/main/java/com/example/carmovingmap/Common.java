package com.example.carmovingmap;

import com.example.carmovingmap.retrofit.IGoogleApi;
import com.example.carmovingmap.retrofit.RetrofitClient;

public class Common {
    public static final String baseUrl = "https://googleapis.com";

    public static IGoogleApi getGoogleApi() {
        return RetrofitClient.getClient(baseUrl).create(IGoogleApi.class);
    }
}
