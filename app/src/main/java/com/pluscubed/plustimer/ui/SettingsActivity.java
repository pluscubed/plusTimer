package com.pluscubed.plustimer.ui;


import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

import com.pluscubed.plustimer.R;

/**
 * Settings Activity and Fragment
 */
public class SettingsActivity extends Activity {
    public static final String PREF_INSPECTION_CHECKBOX = "pref_inspection_checkbox";
    public static final String PREF_HOLDTOSTART_CHECKBOX = "pref_holdtostart_checkbox";
    public static final String PREF_KEEPSCREENON_CHECKBOX = "pref_keepscreenon_checkbox";
    public static final String PREF_TWO_ROW_TIME_CHECKBOX = "pref_two_row_time_checkbox";
    public static final String PREF_TIME_TEXT_SIZE_EDITTEXT = "pref_time_display_size_edittext";
    public static final String PREF_UPDATE_TIME_LIST = "pref_update_time_list";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentManager fm = getFragmentManager();
        Fragment f = fm.findFragmentById(android.R.id.content);
        if (f == null) {
            fm.beginTransaction()
                    .replace(android.R.id.content, new SettingsFragment())
                    .commit();
        }
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        setTitle(R.string.settings);
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

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle paramBundle) {
            super.onCreate(paramBundle);
            addPreferencesFromResource(R.xml.preferences);
            CheckBoxPreference inspection = (CheckBoxPreference) findPreference(PREF_INSPECTION_CHECKBOX);
            inspection.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue.toString().equals("true")) {
                        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean(PREF_HOLDTOSTART_CHECKBOX, true).commit();
                        CheckBoxPreference hold = (CheckBoxPreference) findPreference(PREF_HOLDTOSTART_CHECKBOX);
                        hold.setChecked(true);
                    }
                    return true;
                }
            });
            EditTextPreference size = (EditTextPreference) findPreference(PREF_TIME_TEXT_SIZE_EDITTEXT);
            size.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    EditTextPreference size = (EditTextPreference) preference;
                    size.setSummary(newValue.toString());
                    return true;
                }
            });
            size.setSummary(size.getText());
            ListPreference updateTime = (ListPreference) findPreference(PREF_UPDATE_TIME_LIST);
            updateTime.setEntryValues(new String[]{"0", "1", "2"});
            updateTime.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    ListPreference updateTime = (ListPreference) preference;
                    updateTime.setSummary(updateTime.getEntries()[Integer.parseInt(newValue.toString())]);
                    return true;
                }
            });
            updateTime.setSummary(updateTime.getEntry());
        }
    }
}
