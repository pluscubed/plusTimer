package com.pluscubed.plustimer.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.pluscubed.plustimer.BuildConfig;
import com.pluscubed.plustimer.R;

/**
 * About Page
 */
public class AboutActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentManager fm = getFragmentManager();
        Fragment f = fm.findFragmentById(android.R.id.content);
        if (f == null) {
            fm.beginTransaction()
                    .replace(android.R.id.content, new AboutFragment())
                    .commit();
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        setTitle(R.string.about);
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

    public static class AboutFragment extends Fragment {

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_about, container,
                    false);
            Button licenses = (Button) view.findViewById(R.id
                    .fragment_about_licenses_button);
            licenses.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(),
                            LicensesActivity.class);
                    startActivity(intent);
                }
            });
            TextView appName = (TextView) view.findViewById(R.id
                    .fragment_about_appname_textview);
            appName.setText(getString(R.string.app_name) + "\n v" +
                    BuildConfig.VERSION_NAME);

            Button github = (Button) view.findViewById(R.id
                    .fragment_about_github_button);
            github.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Uri uriUrl = Uri.parse("https://github" +
                            ".com/plusCubed/plusTimer");
                    startActivity(new Intent(Intent.ACTION_VIEW, uriUrl));
                }
            });

            Button email = (Button) view.findViewById(R.id
                    .fragment_about_email_button);
            email.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_SENDTO,
                            Uri.fromParts(
                                    "mailto", "plusCubed@gmail.com", null));
                    startActivity(
                            Intent.createChooser(intent,
                                    "Send email to the developer using..."));
                }
            });

            Button rate = (Button) view.findViewById(R.id
                    .fragment_about_rate_button);
            rate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Uri uri = Uri.parse("market://details?id=com" +
                                ".pluscubed.plustimer");
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                                "http://play.google" +
                                        ".com/store/apps/details?id=com" +
                                        ".pluscubed.plustimer")));
                    }
                }
            });

            return view;
        }
    }
}
