package model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by Hajhosseini on 6/14/2016.
 */
public class MyObject implements Serializable {
    @SerializedName("id")
    public int id;
    @SerializedName("title")
    public String title;
    @SerializedName("description")
    public String description;
}

