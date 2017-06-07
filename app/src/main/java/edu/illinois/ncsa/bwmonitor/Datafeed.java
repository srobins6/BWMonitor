package edu.illinois.ncsa.bwmonitor;

import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by srobins6 on 5/29/17.
 */
class Datafeed {
    String title;
    String type;
    String url;
    ArrayList<String> fields;
    Object response;
    View rootView;

    Datafeed(JSONObject jsonObject) throws JSONException {
        this.title = jsonObject.getString("title");
        this.type = jsonObject.getString("type");
        this.url = jsonObject.getString("url");
        this.fields = new ArrayList<>();
        if (jsonObject.has("fields")) {
            JSONArray fields = jsonObject.getJSONArray("fields");
            int length = fields.length();
            for (int i = 0; i < length; i++) {
                this.fields.add(fields.getString(i));
            }
        }
        this.response = null;
    }

    String getID() {
        return this.title.replaceAll("\\s", "_");
    }
}
