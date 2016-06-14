package webservice;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import model.MyObject;

/**
 * Created by Hajhosseini on 6/14/2016.
 */
public class WebServiceType {

    /**
     * Response Types
     */
    public static final Type STRING = new TypeToken<String>() {}.getType();
    public static final Type INTEGER = new TypeToken<Integer>() {}.getType();
    public static final Type BOOLEAN = new TypeToken<Boolean>() {}.getType();

    public static final Type WEBSERVICE_GET_MY_OBJECT = new TypeToken<MyObject>() {}.getType();
    public static final Type WEBSERVICE_GET_MY_OBJECTS = new TypeToken<ArrayList<MyObject>>() {}.getType();

}
