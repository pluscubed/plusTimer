package com.pluscubed.plustimer.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pluscubed.plustimer.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Licenses Activity
 */
public class LicensesActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentManager fm = getFragmentManager();
        Fragment f = fm.findFragmentById(android.R.id.content);
        if (f == null) {
            fm.beginTransaction()
                    .replace(android.R.id.content, new LicensesFragment())
                    .commit();
        }
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setTitle(R.string.open_source_licenses);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class LicensesFragment extends Fragment {

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_licenses, container, false);
            TextView licensesTextView = (TextView) view
                    .findViewById(R.id.fragment_licenses_textview);

            InputStream inputStream = getActivity().getResources()
                    .openRawResource(R.raw.open_source_licenses);

            InputStreamReader inputreader = new InputStreamReader(inputStream);
            BufferedReader buffreader = new BufferedReader(inputreader);
            String line;
            StringBuilder disclaimerText = new StringBuilder();
            try {
                while ((line = buffreader.readLine()) != null) {
                    disclaimerText.append(line);
                    disclaimerText.append('\n');
                }
            } catch (IOException e) {
                return null;
            }
            licensesTextView.setText(disclaimerText.toString());

            return view;
        }
    }
}
