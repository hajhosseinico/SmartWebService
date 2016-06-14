package ir.hajhosseinico.smartwebservice;

import android.content.Context;
import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Created by Hajhosseini on 6/14/2016.
 */
public class WebService<T> extends Request<T> {

    public static final int CACHE_STATUS_READ_FROM_SERVER = 0;
    public static final int CACHE_STATUS_FALSE = 1;
    public static final int CACHE_STATUS_TRUE = 2;
    public static final int DEFAULT_CACHE = 1000 * 60 * 30; // 30 minutes

    public static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";
    public static final String CONTENT_TYPE_XML = "application/xml; charset=UTF-8";
    public static final String CONTENT_TYPE_FORM_URL = "application/x-www-form-urlencoded; charset=UTF-8";

    private static RequestQueue requestQueue;
    private static RequestQueue sslRequestQueue;
    private Object tag;
    private Map<String, String> params = new HashMap<>();
    private Map<String, String> headerParams = new HashMap<>();
    private int retryPolicyCount = 0;
    private int webserviceTimeOut = 15000;
    private String certificate;
    private int shouldCache = CACHE_STATUS_READ_FROM_SERVER;
    private String cacheKey = "";
    private String url;
    private long cacheTtl = 0;
    private String contentType = null;

    private byte[] bodyContentBytes;
    private Type classType;
    private Context context;
    private OnRequestStateChange onRequestStateChange;

    /**
     * @param _context application of activity context
     * @param _requestMethod  Request.Method.POST || Request.Method.GET
     * @param _url   String url
     * @param _classType Can set in WebServiceType Class. Sample: new TypeToken<String>() {}.getType()
     * @param _onRequestStateChange WebService Response Listener
     * @param _onRequestStateChange onStart: Call before webService hit
     * @param _onRequestStateChange onError: Call when Error happens. returns a volley error object
     * @param _onRequestStateChange onResponse: returns constructor classType
     */
    public WebService(Context _context, int _requestMethod, String _url, Type _classType, final OnRequestStateChange _onRequestStateChange) {
        super(_requestMethod, _url, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                try {
                    if (_onRequestStateChange != null) {
                        _onRequestStateChange.onError(error);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        context = _context;
        classType = _classType;
        onRequestStateChange = _onRequestStateChange;
        url = _url;

        /**
         * Set Timeout and Retry Policy
         */
        DefaultRetryPolicy defaultRetryPolicy = new DefaultRetryPolicy(webserviceTimeOut, retryPolicyCount, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        this.setRetryPolicy(defaultRetryPolicy);
    }

    /**
     * Send params in request body
     * @return
     * @throws AuthFailureError
     */
    @Override
    public byte[] getBody() throws AuthFailureError {
        if (bodyContentBytes != null) {
            return bodyContentBytes;
        } else
            return super.getBody();
    }

    /**
     * Set Cache Key
     * @return
     */
    @Override
    public String getCacheKey() {
        String key = getUrl() + cacheKey;
        return key;
    }

    /**
     * Set request queue type
     * 2 queue because you might want to use different web services in application (requestQueue is static)
     * @return requestQueue or sslRequestQueue
     */
    private RequestQueue getRequestQueue() {
        if (url.startsWith("https") && certificate != null) {
            HurlStack hurlStack = new HurlStack() {
                @Override
                protected HttpsURLConnection createConnection(URL url) throws IOException {
                    HttpsURLConnection httpsURLConnection = (HttpsURLConnection) super.createConnection(url);
                    try {
                        httpsURLConnection.setSSLSocketFactory(getSSLSocketFactory());
                        httpsURLConnection.setHostnameVerifier(getHostnameVerifier());
                        httpsURLConnection.setRequestProperty("Accept-Encoding", "");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return httpsURLConnection;
                }
            };
            if (sslRequestQueue == null) {
                //Initializing RequestQueue
                sslRequestQueue = Volley.newRequestQueue(context.getApplicationContext(), hurlStack);
                return sslRequestQueue;
            }
            else return sslRequestQueue;
        } else if (requestQueue == null) {
            //Initializing RequestQueue
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());
            return requestQueue;
        }else return requestQueue;
    }

    /**
     * Set Header Params
     * @return headerParams if set (or default header params)
     * @throws AuthFailureError
     */
    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        if (headerParams != null) {
            return headerParams;
        } else
            return super.getHeaders();
    }

    /**
     * Set custom bodyContentType
     * @return custom bodyContentType (or default)
     */
    @Override
    public String getBodyContentType() {
        if (contentType != null && !contentType.equals(""))
            return contentType;
        else
            return super.getBodyContentType();
    }

    /**
     * Get request tag;
     */
    private Object getCurrentRequestTag() {
        if (tag == null) {
            this.getUrl();
            tag = this.getUrl();
        }
        return tag;
    }

    /**
     *
     * @param response: Volley NetworkResponse
     * @return Convert response to classType (from constructor)
     */
    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            T t;
            String json;
            Gson gson = new Gson();
            JsonReader reader = new JsonReader(new StringReader(response.toString()));
            reader.setLenient(true);
            json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            t = gson.fromJson(json, classType);

            if (shouldCache == CACHE_STATUS_TRUE) {
                return Response.success(t, new WebServiceCache().customCacheHeaders(response));
            } else if (shouldCache == CACHE_STATUS_FALSE) {
                this.setShouldCache(false);
                return Response.success(t, HttpHeaderParser.parseCacheHeaders(response));
            }
            // case : shouldCache == CACHE_STATUS_READ_FROM_SERVER
            else {
                return Response.success(t, HttpHeaderParser.parseCacheHeaders(response));
            }
        } catch (JsonSyntaxException e) {
            return Response.error(new ParseError(e));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        }
    }

    /**
     * @param response classType object
     */
    @Override
    protected void deliverResponse(T response) {
        if (onRequestStateChange != null) {
            onRequestStateChange.onResponse(response);
        }
    }

    @Override
    public Request<?> setTag(Object tag) {
        this.tag = tag;
        return super.setTag(tag);
    }

    /**
     * @return params
     * @throws AuthFailureError
     */
    @Override
    public Map<String, String> getParams() throws AuthFailureError {
        if (params != null && params.size() > 0) {
            return params;
        } else {
            return super.getParams();
        }
    }

    @Override
    public Response.ErrorListener getErrorListener() {
        return super.getErrorListener();
    }

    @Override
    public void deliverError(VolleyError error) {
        super.deliverError(error);
    }

    /**
     * Add Request To RequestQueue
     */
    public void start() {
        if (onRequestStateChange != null) {
            onRequestStateChange.onStart();
        }

        this.setTag(getCurrentRequestTag());
        getRequestQueue().add(this);
    }

    /**
     * Cancel request
     */
    public void cancel() {
        if (getRequestQueue() != null) {
            getRequestQueue().cancelAll(this.getTag());
        }
    }

    /**
     * Put A Json Formatted String To Parameters Array With "json_key" Key & Passed Json Value
     *
     * @param jsonParams <code>String</code>
     */
    public WebService addParams(String jsonParams) {
        params.put("json_key", jsonParams);
        return this;
    }

    /**
     * WebService Getters
     */

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get Params and Post it to server
     *
     * @param params <code>Map<String, String></code>
     */
    public WebService addParams(Map<String, String> params) {
        this.params = params;
        return this;
    }

    public WebService addParams(String key, String value) {
        params.put(key, value);
        return this;
    }

    public WebService addParams(String key, int value) {
        params.put(key, value + "");
        return this;
    }

    public WebService addParams(String key, float value) {
        params.put(key, value + "");
        return this;
    }

    public WebService addHeaderParams(String key, String value) {
        headerParams.put(key, value);
        return this;
    }

    public WebService addHeaderParams(Map<String, String> params) {
        headerParams = params;
        return this;
    }

    public WebService addHeaderParams(String key, int value) {
        headerParams.put(key, value + "");
        return this;
    }

    public WebService addHeaderParams(String key, float value) {
        headerParams.put(key, value + "");
        return this;
    }

    /**
     * set retry policy count
     */
    public WebService setRetryPolicy(int count) {
        retryPolicyCount = count;
        return this;
    }

    public WebService setTimeOut(int mills) {
        retryPolicyCount = mills;
        return this;
    }

    /**
     * Add custom https certificate
     */
    public WebService addCertificate(String _certificate) {
        certificate = _certificate;
        return this;
    }

    public WebService shouldCache(int _shouldCache, String _cacheKey, long _cacheTtl) {
        shouldCache = _shouldCache;
        cacheKey = _cacheKey;
        cacheTtl = _cacheTtl;
        return this;
    }

    public WebService removeCache() {
        try {
            getRequestQueue().getCache().remove(getCacheKey());
        }catch (Exception e){
            e.printStackTrace();
        }
        return this;
    }

    public WebService setContentType(String _contentType) {
        contentType = _contentType;
        return this;
    }

    /**
     * Add bytes to body
     * @return bodyContentBytes
     */
    public WebService setBodyContentBytes(String _bodyContentBytes){
        bodyContentBytes = _bodyContentBytes.getBytes();
        return this;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * SSL Configuration
     */
    private HostnameVerifier getHostnameVerifier() {
        return new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
//                return hv.verify("your host", session);
                /**
                 * // TODO: check domain
                 */
                return true;
            }
        };
    }

    private SSLSocketFactory getSSLSocketFactory()
            throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, KeyManagementException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream caInput = new ByteArrayInputStream((certificate).getBytes());

        Certificate ca = cf.generateCertificate(caInput);
        caInput.close();

        KeyStore keyStore = KeyStore.getInstance("BKS");
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);

        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);

        TrustManager[] wrappedTrustManagers = getWrappedTrustManagers(tmf.getTrustManagers());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, wrappedTrustManagers, null);

        return sslContext.getSocketFactory();
    }

    private TrustManager[] getWrappedTrustManagers(TrustManager[] trustManagers) {
        final X509TrustManager originalTrustManager = (X509TrustManager) trustManagers[0];
        return new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return originalTrustManager.getAcceptedIssuers();
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        try {
                            originalTrustManager.checkClientTrusted(certs, authType);
//
                        } catch (CertificateException ignored) {
                            ignored.printStackTrace();
                        }
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        try {
                            originalTrustManager.checkServerTrusted(certs, authType);
                        } catch (CertificateException ignored) {
                            ignored.printStackTrace();
                        }
                    }
                }
        };
    }

}