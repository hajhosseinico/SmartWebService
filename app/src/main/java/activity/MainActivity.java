package activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.VolleyError;

import java.util.ArrayList;

import ir.hajhosseinico.smartwebservice.OnRequestStateChange;
import ir.hajhosseinico.smartwebservice.R;
import ir.hajhosseinico.smartwebservice.WebService;
import model.MyObject;
import utility.CommonUtility;
import webservice.WebServiceType;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getMyObject();
        getMyObjectArray();
    }

    private void getMyObjectArray() {
        new WebService(MainActivity.this, Request.Method.POST, CommonUtility.WEBSERVICE_GET_OBJECT_ARRAY, WebServiceType.WEBSERVICE_GET_MY_OBJECTS, new OnRequestStateChange<ArrayList<MyObject>>() {
            @Override
            public void onStart() {
                // start loading
            }

            @Override
            public void onResponse(ArrayList<MyObject> myObjects) {
                for (int i = 0; i < myObjects.size(); i++) {
                    Log.i("LOG", "id : " + myObjects.get(i).id);
                    Log.i("LOG", "title : " + myObjects.get(i).title);
                    Log.i("LOG", "description : " + myObjects.get(i).description);
                }
            }

            @Override
            public void onError(VolleyError error) {

            }
        })
                .addParams("first_key", "first_value")
                .addParams("second_key", 1000)
                .addHeaderParams("header_key", "header_value")
                .setTimeOut(10000)
                .setRetryPolicy(3)
                // set cache
                .shouldCache(WebService.CACHE_STATUS_READ_FROM_SERVER, "my_cache_key", 10000)
                // remove cache if exist
                .removeCache()
                .start();
    }

    private void getMyObject(){
        new WebService(MainActivity.this, Request.Method.POST, CommonUtility.WEBSERVICE_GET_OBJECT, WebServiceType.WEBSERVICE_GET_MY_OBJECTS, new OnRequestStateChange<MyObject>() {
            @Override
            public void onStart() {
                // start loading
            }

            @Override
            public void onResponse(MyObject myObject) {
                // stop loading
                Log.i("LOG", "id : " + myObject.id);
                Log.i("LOG", "title : " + myObject.title);
                Log.i("LOG", "description : " + myObject.description);
            }

            @Override
            public void onError(VolleyError error) {
                error.printStackTrace();
                // stop loading and show retry button
            }
        })
                .addParams("first_key", "first_value")
                .addParams("second_key", 1000)
                .addHeaderParams("header_key", "header_value")
                .setTimeOut(10000)
                .setRetryPolicy(3)
                // set cache
                .shouldCache(WebService.CACHE_STATUS_READ_FROM_SERVER, "my_cache_key", 10000)
                // remove cache if exist
                .removeCache()
                .start();
    }
}
