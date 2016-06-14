package ir.hajhosseinico.smartwebservice;

import com.android.volley.VolleyError;

/**
 * Created by Hajhosseini on 6/14/2016.
 */
public interface OnRequestStateChange<T> {
    void onStart();
    void onResponse(T t);
    void onError(VolleyError error);
}
