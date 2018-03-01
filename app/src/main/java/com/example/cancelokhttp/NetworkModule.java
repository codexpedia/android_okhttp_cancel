package com.example.cancelokhttp;


import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public enum NetworkModule {
    INSTANCE;
    private static final String TAG = "NetworkModule";
    private static final long CONNECT_TIMEOUT = 20000;   // 20 seconds
    private static final long READ_TIMEOUT = 20000;      // 20 seconds
    private static OkHttpClient okHttpClient = null;
    private static String USER_AGENT           = "search-android/1.0";
    private static final String API_ACCEPT_HEADER    = "application/json";
    private static boolean requestWasCanceled = false;
    private static final String TAG_SEARCH = "search";

    /**
     * Method to build and return an OkHttpClient so we can set/get
     * headers quickly and efficiently.
     * @return OkHttpClient
     */
    private OkHttpClient getClient() {
        if (okHttpClient != null) return okHttpClient;

        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS);

        // Logging interceptor
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        okHttpClientBuilder.addInterceptor(httpLoggingInterceptor);

        // custom interceptor for adding header and NetworkMonitor sliding window
        okHttpClientBuilder.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                // Add whatever we want to our request headers.
                Request request = chain.request().newBuilder().addHeader("Accept", "application/json").build();

                Response response;
                boolean isSuccess = false;
                try {
                    response = chain.proceed(request);
                    isSuccess = response.isSuccessful();
                } catch (SocketTimeoutException | UnknownHostException | SocketException e) {
                    e.printStackTrace();
                    isSuccess = false;
                    Log.d(TAG, "network call exception: " + e.getMessage());
                    throw new IOException(e);
                } finally {
                    // push the network check sliding widow after a network call
                    Log.d(TAG, "network call was successful: " + isSuccess);
                    Log.d(TAG, "requestWasCanceled: " + requestWasCanceled);
                    requestWasCanceled = false;
                }


                return response;
            }
        });
        okHttpClient = okHttpClientBuilder.build();
        return  okHttpClient;
    }


    private Request.Builder buildRequest(URL url) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .header("Accept", API_ACCEPT_HEADER)
                .header("Content-Type", API_ACCEPT_HEADER)
                .header("User-Agent", USER_AGENT);

        Log.d(TAG, "buildRequest: " + url.toString());

        if(url.toString().contains("http://search.example.com/api/search/")) {
            builder.tag(TAG_SEARCH + System.currentTimeMillis());
        }

        return builder;
    }

    private Request.Builder buildRequest(URL url, String credential) {
        return buildRequest(url).header("Authorization", credential);
    }

    private URL buildURL(Uri builtUrl) {
        if (builtUrl == null) return null;
        try {
            String urlStr = builtUrl.toString();
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private URL buildURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private URL buildUrlWithQueryParameters(String endpoint, Map<String, String> queries) {
        Uri.Builder uriBuilder = Uri.parse(endpoint).buildUpon();
        for (Map.Entry<String, String> query : queries.entrySet()) {
            uriBuilder.appendQueryParameter(query.getKey(), query.getValue());
        }
        Uri uri = uriBuilder.build();
        return buildURL(uri);
    }

    private String getData(Request request) {
        OkHttpClient client = getClient();
        try {
            cancelCallWithTag(TAG_SEARCH);
            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Get string without an authentication
     * @param endpoint the url to get the data from
     * @return String
     */
    public String getString(String endpoint) {
        Request request = buildRequest(buildURL(endpoint))
                .build();
        return getData(request);
    }

    /**
     * Get string by username password
     * @param endpoint rest url
     * @param username username
     * @param password password
     * @return String
     */
    public String getString(String endpoint, String username, String password) {
        String credentials = username + ":" + password;
        final String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

        Request request = buildRequest(buildURL(endpoint), basicAuth).build();
        return getData(request);
    }

    /**
     * Get string by bearer token
     * @param endpoint rest url
     * @param token bearer token
     * @return String
     */
    public String getString(String endpoint, String token) {
        String credentials = "Bearer " + token;
        Request request = buildRequest(buildURL(endpoint), credentials).build();
        return getData(request);
    }


    /**
     * Get string by bear token with etag
     * @param endpoint rest url
     * @param token Bearer token
     * @param etag  If-None-Match header
     * @return String
     */
    public String getStringWithEtag(String endpoint, String token, String etag) {
        String credentials = "Bearer " + token;
        if(!etag.matches("/^\".*\"$/")) {
            etag = "\"" + etag + "\"";
        }
        Request request = buildRequest(buildURL(endpoint), credentials).header("If-None-Match", etag).build();
        return getData(request);
    }


    /**
     * Get string by bearer token and query parameters
     * @param endpoint rest url
     * @param queries url query parameters
     * @return String
     */
    public String getString(String endpoint, Map<String, String> queries) {
        URL url = buildUrlWithQueryParameters(endpoint, queries);
        Request request = buildRequest(url).build();
        return getData(request);
    }


    /**
     * Get string by bearer token and query parameters
     * @param endpoint rest url
     * @param token Bearer token
     * @param queries url query parameters
     * @return String
     */
    public String getString(String endpoint, String token, Map<String, String> queries) {
        URL url = buildUrlWithQueryParameters(endpoint, queries);
        String credentials = "Bearer " + token;
        Request request = buildRequest(url, credentials).build();
        return getData(request);
    }


    public void cancelCallWithTag(String tag) {
        Log.d("cancelCallWithTag", "cancelCallWithTag>>>" + tag);

        Log.d("cancelCallWithTag", "client.dispatcher().queuedCalls().size()>>>" + okHttpClient.dispatcher().queuedCalls().size());
        Log.d("cancelCallWithTag", "client.dispatcher().runningCalls().size()>>>" + okHttpClient.dispatcher().runningCalls().size());


        for(Call call : okHttpClient.dispatcher().queuedCalls()) {
            Log.d("cancelCallWithTag", "queuedCalls call.request().tag()>>>>>" + call.request().tag());
            if (call.request().tag().toString().contains(tag)) {
                call.cancel();
                requestWasCanceled = true;
                Log.d("cancelCallWithTag", "call.cancel()>>>>>" + call.request().tag());
            }
        }

        for(Call call : okHttpClient.dispatcher().runningCalls()) {
            Log.d("cancelCallWithTag", "runningCalls call.request().tag()>>>>>" + call.request().tag());
            if (call.request().toString().contains(tag)) {
                call.cancel();
                requestWasCanceled = true;
                Log.d("cancelCallWithTag", "runningCalls call.cancel()>>>>>" + call.request().tag());
            }
        }
    }
}
