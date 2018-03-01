package com.example.cancelokhttp;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    TextView tvDisplay;
    NetworkTask networkTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvDisplay = (TextView) findViewById(R.id.tv_display);
    }


    public void doNetworkCall(View v) {
        if (networkTask != null) networkTask.cancel(true);

        networkTask = new NetworkTask();
        networkTask.execute();
    }

    public void cancelNetworkCall(View v) {
        networkTask.cancel(true);
        NetworkModule.INSTANCE.cancelCallWithTag("search");
    }

    public void clearDisplay(View v) {
        tvDisplay.setText("");
    }

    private class NetworkTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            return NetworkModule.INSTANCE.getString("http://search.example.com/api/search/?query=*food*&type=fruit");
        }
        @Override
        protected void onPostExecute(String data) {
            super.onPostExecute(data);
            tvDisplay.setText( System.currentTimeMillis() + "\n" + data);
        }
    }
}
