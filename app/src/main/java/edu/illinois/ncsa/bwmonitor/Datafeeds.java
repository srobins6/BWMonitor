package edu.illinois.ncsa.bwmonitor;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Datafeeds extends AppCompatActivity {
    public static String PREFS_NAME = "Datafeeds";
    ExpandableListView expandableListView;
    ExpandableListAdapter expandableListAdapter;
    List<String> expandableListTitle;
    HashMap<String, List<Datafeed>> expandableListDetail;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof AutoCompleteTextView) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private HashMap<String, List<Datafeed>> getData(JSONObject response) throws JSONException {
        HashMap<String, List<Datafeed>> expandableListDetail = new HashMap<>();
        JSONArray datafeeds = response.getJSONArray("datafeeds");
        int datafeedsLength = datafeeds.length();
        for (int i = 0; i < datafeedsLength; i++) {
            JSONObject datafeedObject = datafeeds.getJSONObject(i);
            String category = datafeedObject.getString("category");
            List<Datafeed> categoryList = expandableListDetail.get(category);
            if (categoryList == null) {
                categoryList = new ArrayList<>();
                expandableListDetail.put(category, categoryList);
            }
            categoryList.add(new Datafeed(datafeedObject));
        }
        return expandableListDetail;
    }

    public void updateAvailableDatafeeds() {
        RequestQueue queue = Volley.newRequestQueue(this);
        final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String datafeedSourceURL = settings.getString("data_source_url", "");
        if (!datafeedSourceURL.matches("https?://.*")) {
            datafeedSourceURL = "http://" + datafeedSourceURL;
        }
        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, datafeedSourceURL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    float responseVersion = (float) response.getDouble("version");
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putFloat("data_source_url_version", responseVersion);
                    editor.apply();
                    expandableListView = (ExpandableListView) findViewById(R.id.datafeeds_list);
                    expandableListDetail = getData(response);
                    expandableListTitle = new ArrayList<>(expandableListDetail.keySet());
                    expandableListAdapter = new DatafeedListAdapter(getApplicationContext(), expandableListTitle, expandableListDetail);
                    expandableListView.setAdapter(expandableListAdapter);
                    int groups = expandableListAdapter.getGroupCount();
                    for (int groupPosition = 0; groupPosition < groups; groupPosition++) {
                        expandableListView.expandGroup(groupPosition, false);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                AlertDialog alertDialog = new AlertDialog.Builder(Datafeeds.this).create();
                alertDialog.setTitle("Error");
                alertDialog.setMessage("Invalid Datafeed Source URL");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                alertDialog.show();
            }
        });
        queue.add(jsObjRequest);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datafeeds);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String datafeedSourceURL = settings.getString("data_source_url", "");
        AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.datafeed_source_url_autocomplete);
        if (datafeedSourceURL.length() > 0) {
            textView.setText(datafeedSourceURL);
        }
        String[] sources = getResources().getStringArray(R.array.datafeed_sources);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sources);
        textView.setAdapter(adapter);
        textView.setThreshold(1);
        textView.setHorizontallyScrolling(true);
        textView.setSingleLine(true);
        textView.setOnFocusChangeListener(new AutoCompleteTextView.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && ((AutoCompleteTextView) v).getText().toString().length() == 0) {
                    ((AutoCompleteTextView) v).showDropDown();
                } else if (!hasFocus) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        });
        textView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ((AutoCompleteTextView) v).showDropDown();
                return false;
            }
        });
        Intent intent = getIntent();
        boolean updateFeeds = intent.getBooleanExtra("update_feeds", false);
        if (updateFeeds) {
            updateAvailableDatafeeds();
        }
    }

    public void updateAvailableDatafeedsClick(View view) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.datafeed_source_url_autocomplete);
        editor.putString("data_source_url", textView.getText().toString());
        editor.apply();
        updateAvailableDatafeeds();
    }

    private class DatafeedListAdapter extends BaseExpandableListAdapter {
        private Context context;
        private List<String> expandableListTitle;
        private HashMap<String, List<Datafeed>> expandableListDetail;

        DatafeedListAdapter(Context context, List<String> expandableListTitle, HashMap<String, List<Datafeed>> expandableListDetail) {
            this.context = context;
            this.expandableListTitle = expandableListTitle;
            this.expandableListDetail = expandableListDetail;
        }

        @Override
        public Object getChild(int listPosition, int expandedListPosition) {
            return this.expandableListDetail.get(this.expandableListTitle.get(listPosition)).get(expandedListPosition);
        }

        @Override
        public long getChildId(int listPosition, int expandedListPosition) {
            return expandedListPosition;
        }

        @Override
        public View getChildView(int listPosition, int expandedListPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            final Datafeed datafeed = (Datafeed) getChild(listPosition, expandedListPosition);
            final String dataFeedsPreferenceName = "data_feeds";
            final Set<String> subscribedDatafeeds = settings.getStringSet(dataFeedsPreferenceName, new HashSet<String>());
            String expandedListText = datafeed.title;
            final String url = datafeed.url;
            if (convertView == null) {
                LayoutInflater layoutInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.list_item, null);
            }
            CheckBox expandedListCheckBoxView = (CheckBox) convertView.findViewById(R.id.expandedListItem);
            expandedListCheckBoxView.setText(expandedListText);
            if (subscribedDatafeeds.contains(url)) {
                expandedListCheckBoxView.setChecked(true);
            }
            expandedListCheckBoxView.setOnClickListener(new CheckBox.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean checked = ((CheckBox) v).isChecked();
                    if (checked) {
                        if (datafeed.type.equals("notification")) {
                            FirebaseMessaging.getInstance().subscribeToTopic(datafeed.getID());
                        }
                        subscribedDatafeeds.add(url);
                    } else {
                        if (datafeed.type.equals("notification")) {
                            FirebaseMessaging.getInstance().unsubscribeFromTopic(datafeed.getID());
                        }
                        subscribedDatafeeds.remove(url);
                    }
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putStringSet(dataFeedsPreferenceName, subscribedDatafeeds);
                    editor.putInt(dataFeedsPreferenceName + "subscribed_count", subscribedDatafeeds.size());
                    editor.apply();
                }
            });
            return convertView;
        }

        @Override
        public int getChildrenCount(int listPosition) {
            return this.expandableListDetail.get(this.expandableListTitle.get(listPosition)).size();
        }

        @Override
        public Object getGroup(int listPosition) {
            return this.expandableListTitle.get(listPosition);
        }

        @Override
        public int getGroupCount() {
            return this.expandableListTitle.size();
        }

        @Override
        public long getGroupId(int listPosition) {
            return listPosition;
        }

        @Override
        public View getGroupView(int listPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            String listTitle = (String) getGroup(listPosition);
            if (convertView == null) {
                LayoutInflater layoutInflater = (LayoutInflater) this.context.
                                                                                     getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.list_group, null);
            }
            TextView listTitleTextView = (TextView) convertView.findViewById(R.id.listTitle);
            listTitleTextView.setTypeface(null, Typeface.BOLD);
            listTitleTextView.setText(listTitle);
            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int listPosition, int expandedListPosition) {
            return true;
        }
    }
}
