package com.rayworks.droidweekly.data;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * * Taken from
 * https://stackoverflow.com/questions/26509107/how-to-specify-a-default-user-agent-for-okhttp-2-x-requests
 */
public class AgentInterceptor implements Interceptor {
    private final String userAgent;

    public AgentInterceptor(String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request requestWithUserAgent =
                originalRequest.newBuilder().header("User-Agent", userAgent).build();
        return chain.proceed(requestWithUserAgent);
    }
}
