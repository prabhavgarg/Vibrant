package com.vibrant.boss.vibrant;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ArrayList<Events> arrayList;
    private ListView listView;
    private NewsAdapter adapter;
    TextToSpeech toSpeech;
    LinearLayout linearLayout, background;
    ProgressBar progress;
    int result = 0;
    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final String USGS_REQUEST_URL = "https://newsapi.org/v1/articles?source=the-next-web&sortBy=latest&apiKey=5e8c7b9857db46fd8c6bbc2b03d9fde5";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progress=(ProgressBar)findViewById(R.id.progressbar);
        progress.setVisibility(View.VISIBLE);
        linearLayout = (LinearLayout)findViewById(R.id.layout_all);
        background = (LinearLayout)findViewById(R.id.background);

        toSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) result = toSpeech.setLanguage(Locale.UK);
                else {
                    Toast.makeText(getApplicationContext(), "Feature not support in your device", Toast.LENGTH_SHORT).show();
                }
            }
        });

        arrayList = new ArrayList<>();
        adapter = new NewsAdapter(MainActivity.this, arrayList);
        listView = (ListView) findViewById(R.id.listview_newspaper);
        listView.setAdapter(adapter);
        // Kick off an {@link AsyncTask} to perform the network request
        TsunamiAsyncTask task = new TsunamiAsyncTask();
        task.execute();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (toSpeech != null) {
                    toSpeech.stop();
                }
                toSpeech.speak(arrayList.get(position).getTitle(), TextToSpeech.QUEUE_FLUSH, null);
                TextView textView1 = (TextView)findViewById(R.id.news_title);
                TextView textView2 = (TextView)findViewById(R.id.news_description);
                textView1.setText(arrayList.get(position).getTitle());
                textView2.setText(arrayList.get(position).getDescription());

            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (toSpeech != null) {
                    toSpeech.stop();
                }
                toSpeech.speak(arrayList.get(position).getDescription(), TextToSpeech.QUEUE_FLUSH, null);
                return false;
            }
        });
    }

    private void updateUi(Events news) {
        ListView listView = (ListView) findViewById(R.id.listview_newspaper);
        listView.setAdapter(adapter);

        // Display the earthquake date in the UI
        TextView title = (TextView) findViewById(R.id.news_title);
        title.setText(news.title);

        progress.setVisibility(View.GONE);
        linearLayout.setVisibility(View.VISIBLE);


        // Display whether or not there was a tsunami alert in the UI
        TextView desc = (TextView) findViewById(R.id.news_description);
        desc.setText(news.description);
        // Display whether or not there was a tsunami alert in the UI
//        TextView time = (TextView) findViewById(R.id.news_time);
//        time.setText(news.publishedAt);
    }

    /**
     * Returns a formatted date and time string for when the earthquake happened.
     */
    private String getDateString(long timeInMilliseconds) {
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy 'at' HH:mm:ss z");
        return formatter.format(timeInMilliseconds);
    }

    /**
     * Return the display string for whether or not there was a tsunami alert for an earthquake.
     */
    private String getTsunamiAlertString(int tsunamiAlert) {
        switch (tsunamiAlert) {
            case 0:
                return getString(0);
            case 1:
                return getString(0);
            default:
                return getString(0);
        }
    }

    /**
     * {@link AsyncTask} to perform the network request on a background thread, and then
     * update the UI with the first earthquake in the response.
     */
    private class TsunamiAsyncTask extends AsyncTask<URL, Void, Events> {

        @Override
        protected Events doInBackground(URL... urls) {
            // Create URL object
            URL url = createUrl(USGS_REQUEST_URL);

            // Perform HTTP request to the URL and receive a JSON response back
            String jsonResponse = "";
            try {
                jsonResponse = makeHttpRequest(url);
            } catch (IOException e) {
                // TODO Handle the IOException
            }

            // Extract relevant fields from the JSON response and create an {@link Event} object
            Events news = extractFeatureFromJson(jsonResponse);
            // Return the {@link Event} object as the result fo the {@link TsunamiAsyncTask}
            return news;
        }

        /**
         * Update the screen with the given earthquake (which was the result of the
         * {@link TsunamiAsyncTask}).
         */
        @Override
        protected void onPostExecute(Events news) {
            if (news == null) {
                return;
            }

            updateUi(news);
        }

        /**
         * Returns new URL object from the given string URL.
         */
        private URL createUrl(String stringUrl) {
            URL url = null;
            try {
                url = new URL(stringUrl);
            } catch (MalformedURLException exception) {
                Log.e(LOG_TAG, "Error with creating URL", exception);
                return null;
            }
            return url;
        }

        /**
         * Make an HTTP request to the given URL and return a String as the response.
         */
        private String makeHttpRequest(URL url) throws IOException {
            String jsonResponse = "";
            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(100000 /* milliseconds */);
                urlConnection.setConnectTimeout(150000 /* milliseconds */);
                urlConnection.connect();
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } catch (IOException e) {
                // TODO: Handle the exception
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (inputStream != null) {
                    // function must handle java.io.IOException here
                    inputStream.close();
                }
            }
            return jsonResponse;
        }

        /**
         * Convert the {@link InputStream} into a String which contains the
         * whole JSON response from the server.
         */
        private String readFromStream(InputStream inputStream) throws IOException {
            StringBuilder output = new StringBuilder();
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line = reader.readLine();
                while (line != null) {
                    output.append(line);
                    line = reader.readLine();
                }
            }
            return output.toString();
        }

        /**
         * Return an {@link Events} object by parsing out information
         * about the first earthquake from the input earthquakeJSON string.
         */
        private Events extractFeatureFromJson(String earthquakeJSON) {
            try {
                JSONObject baseJsonResponse = new JSONObject(earthquakeJSON);
                JSONArray articles = baseJsonResponse.getJSONArray("articles");
                int a = articles.length();
                JSONObject jsonObject;
                String author = "", description = "", publishedAt = "", imageUrl = "", title = "";
                for (int i = 0; i < articles.length(); i++) {
                    // Extract out the title, time, and tsunami values
                    jsonObject = articles.getJSONObject(i);
                    author = jsonObject.getString("author");
                    title = jsonObject.getString("title");
                    description = jsonObject.getString("description");
                    publishedAt = jsonObject.getString("publishedAt");
                    imageUrl = jsonObject.getString("urlToImage");

                    // Create a new {@link Event} object
                    arrayList.add(new Events(author, title, description, publishedAt, imageUrl));
                }
                return new Events(author, title, description, publishedAt, imageUrl);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Problem parsing the earthquake JSON results", e);
            }
            return null;
        }
    }
}
