package com.vectorcoder.androidwoocommerce.oauth;

import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by muneeb.vectorcoder@gmail.com on 30-Jan-18.
 */

public class BasicOAuth implements Interceptor {
    
    private static final String OAUTH_CONSUMER_KEY = "consumer_key";
    private static final String OAUTH_CONSUMER_SECRET = "consumer_secret";
    
    private final String consumerKey;
    private final String consumerSecret;
    
    
    private BasicOAuth(String consumerKey, String consumerSecret) {
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
    }
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        HttpUrl originalHttpUrl = original.url();
        
        Log.d("URL", original.url().toString());
        Log.d("method", ""+original.method());
        
        ////////////////////////////////////////////////////////////
        
        
        String basicOAuthString = OAUTH_CONSUMER_KEY + "=" + consumerKey + "&" + OAUTH_CONSUMER_SECRET + "=" + consumerSecret;
        Log.d("basicOAuthString", "basicOAuthString="+basicOAuthString);
        
        HttpUrl url = originalHttpUrl.newBuilder()
                .addQueryParameter(OAUTH_CONSUMER_KEY, consumerKey)
                .addQueryParameter(OAUTH_CONSUMER_SECRET, consumerSecret)
                .build();
        
        
        // Request customization: add request headers
        Request.Builder requestBuilder = original.newBuilder()
                .addHeader("Content-Type", "application/json")
                .url(url);
        
        Request request = requestBuilder.build();
        return chain.proceed(request);
    }
    
    
    public static final class Builder {
        
        private String consumerKey;
        private String consumerSecret;
        private int type;
        
        public Builder consumerKey(String consumerKey) {
            if (consumerKey == null) throw new NullPointerException("consumerKey = null");
            this.consumerKey = consumerKey;
            return this;
        }
        
        public Builder consumerSecret(String consumerSecret) {
            if (consumerSecret == null) throw new NullPointerException("consumerSecret = null");
            this.consumerSecret = consumerSecret;
            return this;
        }
        
        
        
        public BasicOAuth build() {
            
            if (consumerKey == null) throw new IllegalStateException("consumerKey not set");
            if (consumerSecret == null) throw new IllegalStateException("consumerSecret not set");
            
            return new BasicOAuth(consumerKey, consumerSecret);
        }
    }
    
    public String urlEncoded(String url) {
        String encodedurl = "";
        try {
            
            encodedurl = URLEncoder.encode(url, "UTF-8");
            Log.d("TEST", encodedurl);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        
        return encodedurl;
    }
}
