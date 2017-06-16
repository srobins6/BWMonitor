package edu.illinois.ncsa.bwmonitor;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ViewData extends AppCompatActivity {
    public static final String PREFS_NAME = "Datafeeds";
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private List<Datafeed> datafeeds;
    private RequestQueue queue;

    private static void updateDatafeedView(final Datafeed datafeed, RequestQueue queue) {
        final View rootView = datafeed.rootView;
        if (rootView == null) {
            return;
        }
        String datafeedURL = datafeed.url;
        if (!datafeedURL.matches("https?://.*")) {
            datafeedURL = "http://" + datafeedURL;
        }
        switch (datafeed.type) {
            case "text": {
                StringRequest stringRequest = new StringRequest(Request.Method.GET, datafeedURL, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (datafeed.response == null || datafeed.response != response) {
                            datafeed.response = response;
                            TextView textView = (TextView) rootView.findViewById(R.id.datafeed_text);
                            if (textView != null) {
                                textView.setText(response);
                            }
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                });
                queue.add(stringRequest);
                break;
            }
            case "html": {
                final String finalDatafeedURL = datafeedURL;
                StringRequest stringRequest = new StringRequest(Request.Method.GET, datafeedURL, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (datafeed.response == null || datafeed.response != response) {
                            datafeed.response = response;
                            final WebView webView = (WebView) rootView.findViewById(R.id.datafeed_html);
                            if (webView != null) {
                                webView.loadUrl(finalDatafeedURL);
                            }
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                });
                queue.add(stringRequest);
                break;
            }
            case "piechart": {
                final PieChart pieChart = (PieChart) rootView.findViewById(R.id.datafeed_piechart);
                JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, datafeedURL, null, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        if (datafeed.response == null || datafeed.response != response) {
                            datafeed.response = response;
                            try {
                                updatePieChart(response, datafeed, pieChart);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                });
                queue.add(jsonArrayRequest);
                break;
            }
            case "linechart": {
                final LineChart lineChart = (LineChart) rootView.findViewById(R.id.datafeed_linechart);
                JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, datafeedURL, null, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        if (datafeed.response == null || datafeed.response != response) {
                            datafeed.response = response;
                            try {
                                updateLineChart(response, false, datafeed, lineChart);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                });
                queue.add(jsonArrayRequest);
                break;
            }
            case "timechart": {
                final LineChart lineChart = (LineChart) rootView.findViewById(R.id.datafeed_linechart);
                JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, datafeedURL, null, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        if (datafeed.response == null || datafeed.response != response) {
                            datafeed.response = response;
                            try {
                                updateLineChart(response, true, datafeed, lineChart);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                });
                queue.add(jsonArrayRequest);
                break;
            }
        }
    }

    private static void updatePieChart(JSONArray response, Datafeed datafeed, PieChart mChart) throws JSONException {
        List<PieEntry> entries = new ArrayList<>();
        int length = response.length();
        for (int i = 0; i < length; i++) {
            float value = ((Number) response.get(i)).floatValue();
            entries.add(new PieEntry(value, datafeed.fields.get(i)));
        }
        PieDataSet set = new PieDataSet(entries, datafeed.title);
        set.setColors(ColorTemplate.VORDIPLOM_COLORS);
        set.setValueTextSize(8f);
        PieData data = new PieData(set);
        mChart.setEntryLabelColor(Color.BLACK);
        mChart.setEntryLabelTextSize(10f);
        mChart.setDrawHoleEnabled(false);
        mChart.setData(data);
        mChart.setTouchEnabled(false);
        Legend legend = mChart.getLegend();
        legend.setEnabled(false);
        mChart.invalidate();
    }

    private static void updateLineChart(JSONArray response, boolean timeChart, Datafeed datafeed, LineChart mChart) throws JSONException {
        SparseArray<List<Entry>> entries = new SparseArray<>();
        int length = response.length();
        int fieldsLength = datafeed.fields.size();
        for (int j = 1; j < fieldsLength; j++) {
            entries.put(j, new ArrayList<Entry>());
        }
        for (int i = 0; i < length; i++) {
            JSONArray dataPoint = response.getJSONArray(i);
            float xValue = ((Number) dataPoint.get(0)).floatValue();
            for (int j = 1; j < fieldsLength; j++) {
                List<Entry> listEntries = entries.get(j);
                float yValue = ((Number) dataPoint.get(j)).floatValue();
                listEntries.add(new Entry(xValue, yValue));
            }
        }
        List<ILineDataSet> dataSets = new ArrayList<>();
        for (int j = 1; j < fieldsLength; j++) {
            LineDataSet set = new LineDataSet(entries.get(j), datafeed.fields.get(j));
            set.setValueTextSize(0);
            dataSets.add(set);
        }
        mChart.getDescription().setPosition(0, 0);
        LineData data = new LineData(dataSets);
        Legend legend = mChart.getLegend();
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setEnabled(true);
        XAxis xAxis = mChart.getXAxis();
        xAxis.setGranularity(1f);
        if (timeChart) {
            xAxis.setValueFormatter(new DateTimeFormatter());
        }
        mChart.setData(data);
        mChart.invalidate();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        JodaTimeAndroid.init(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_data);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String datafeedSourceURL = settings.getString("data_source_url", "");
        final float datafeedURLVersion = settings.getFloat("data_source_url_version", 0);
        boolean updateFeeds;
        final Intent intent = new Intent(this, Datafeeds.class);
        if (datafeedSourceURL.length() == 0) {
            updateFeeds = true;
        } else {
            if (!datafeedSourceURL.matches("https?://.*")) {
                datafeedSourceURL = "http://" + datafeedSourceURL;
            }
            queue = Volley.newRequestQueue(this);
            JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, datafeedSourceURL, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        float responseVersion = (float) response.getDouble("version");
                        boolean newFeedsAvailable = responseVersion != datafeedURLVersion;
                        if (newFeedsAvailable) {
                            AlertDialog alertDialog = new AlertDialog.Builder(ViewData.this).create();
                            alertDialog.setTitle("Alert");
                            alertDialog.setMessage("New Feeds Available");
                            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    intent.putExtra("update_feeds", true);
                                    startActivity(intent);
                                }
                            });
                            alertDialog.show();
                        } else {
                            // Create the adapter that will return a fragment for each of the three
                            // primary sections of the activity.
                            JSONArray allDatafeeds = response.getJSONArray("datafeeds");
                            datafeeds = new ArrayList<>();
                            int length = allDatafeeds.length();
                            final Set<String> subscribedDatafeeds = settings.getStringSet("data_feeds", new HashSet<String>());
                            for (int i = 0; i < length; i++) {
                                String datafeedURL = allDatafeeds.getJSONObject(i).getString("url");
                                if (subscribedDatafeeds.contains(datafeedURL)) {
                                    Datafeed datafeed = new Datafeed(allDatafeeds.getJSONObject(i));
                                    if (!datafeed.type.equals("notification")) {
                                        datafeeds.add(datafeed);
                                    }
                                }
                            }
                            if (datafeeds.size() > 0) {
                                toolbar.setTitle(datafeeds.get(0).title);
                            } else {
                                intent.putExtra("update_feeds", true);
                                startActivity(intent);
                            }
                            mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
                            // Set up the ViewPager with the sections adapter.
                            mViewPager = (ViewPager) findViewById(R.id.container);
                            mViewPager.setAdapter(mSectionsPagerAdapter);
                            TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
                            tabLayout.setupWithViewPager(mViewPager);
                            mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                                @Override
                                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                                }

                                @Override
                                public void onPageSelected(int position) {
                                    Datafeed datafeed = datafeeds.get(position);
                                    String datafeedTitle = datafeed.title;
                                    toolbar.setTitle(datafeedTitle);
                                    updateDatafeedView(datafeed, queue);
                                }

                                @Override
                                public void onPageScrollStateChanged(int state) {
                                }
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    AlertDialog alertDialog = new AlertDialog.Builder(ViewData.this).create();
                    alertDialog.setTitle("Error");
                    alertDialog.setMessage("Invalid Datafeed Source URL");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            intent.putExtra("update_feeds", false);
                            startActivity(intent);
                        }
                    });
                    alertDialog.show();
                }
            });
            queue.add(jsObjRequest);
            updateFeeds = false;
        }
        if (updateFeeds) {
            intent.putExtra("update_feeds", false);
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_view_data, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.datafeeds_setting) {
            final Intent intent = new Intent(this, Datafeeds.class);
            intent.putExtra("update_feeds", true);
            startActivity(intent);
        } else if (id == R.id.datafeeds_refresh) {
            int position = mViewPager.getCurrentItem();
            Datafeed datafeed = datafeeds.get(position);
            updateDatafeedView(datafeed, queue);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            int index = getArguments().getInt(ARG_SECTION_NUMBER, 1) - 1;
            Datafeed datafeed = ((ViewData) getActivity()).datafeeds.get(index);
            String datafeedType = datafeed.type;
            RequestQueue queue = Volley.newRequestQueue(getContext());
            final View rootView;
            switch (datafeedType) {
                case "text": {
                    rootView = inflater.inflate(R.layout.datafeed_text, container, false);
                    break;
                }
                case "html": {
                    rootView = inflater.inflate(R.layout.datafeed_html, container, false);
                    break;
                }
                case "piechart": {
                    rootView = inflater.inflate(R.layout.datafeed_piechart, container, false);
                    break;
                }
                case "linechart": {
                    rootView = inflater.inflate(R.layout.datafeed_linechart, container, false);
                    break;
                }
                case "timechart": {
                    rootView = inflater.inflate(R.layout.datafeed_linechart, container, false);
                    break;
                }
                default: {
                    rootView = inflater.inflate(R.layout.datafeed_text, container, false);
                    break;
                }
            }
            datafeed.rootView = rootView;
            updateDatafeedView(datafeed, queue);
            return rootView;
        }
    }

    private static class DateTimeFormatter implements IAxisValueFormatter {
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            LocalTime date = new DateTime((long) value * 1000).toLocalTime();
            String hour = date.toString("h");
            String minute = date.toString("m");
            if (minute.length() < 2) {
                minute = "0" + minute;
            }
            return hour + ":" + minute + date.toString("a");
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    private class SectionsPagerAdapter extends FragmentPagerAdapter {
        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            return datafeeds.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return datafeeds.get(position).title;
        }
    }
}
