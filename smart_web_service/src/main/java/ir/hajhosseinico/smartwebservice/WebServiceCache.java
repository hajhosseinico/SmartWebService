package ir.hajhosseinico.smartwebservice;

import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.toolbox.HttpHeaderParser;

import java.util.Map;

/**
 * Created by Hajhosseini on 6/14/2016.
 */
public class WebServiceCache {
    /**
     * Custom cache header
     * @param response
     * @return
     */
    public Cache.Entry customCacheHeaders(NetworkResponse response) {
        long now = System.currentTimeMillis();

        Map<String, String> headers = response.headers;
        long serverDate = 0;
        String headerValue;

        headerValue = headers.get("Date");
        if (headerValue != null) {
            serverDate = HttpHeaderParser.parseDateAsEpoch(headerValue);
        }

        final long cacheHitButRefreshed = 5 * 60 * 1000; // in 5 minutes cache will be hit, but also refreshed on background
        final long cacheExpired =  24 * 60 * 60 * 1000; // in 24 hours this cache entry expires completely
        final long softExpire = now + cacheHitButRefreshed;
        final long ttl = now + cacheExpired;

        Cache.Entry entry = HttpHeaderParser.parseCacheHeaders(response);
        if(entry == null)
            entry = new Cache.Entry();
        entry.softTtl = softExpire;
        entry.ttl = ttl;
        entry.serverDate = serverDate;
        entry.responseHeaders = headers;

        return entry;
    }

  }
