package com.pluscubed.plustimer.ui;


import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.widget.Toast;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Settings Activity and Fragment
 */
public class SettingsActivity extends ActionBarActivity {

    public static final String PREF_INSPECTION_CHECKBOX =
            "pref_inspection_checkbox";
    public static final String PREF_HOLDTOSTART_CHECKBOX =
            "pref_holdtostart_checkbox";
    public static final String PREF_KEEPSCREENON_CHECKBOX =
            "pref_keepscreenon_checkbox";
    public static final String PREF_TWO_ROW_TIME_CHECKBOX =
            "pref_two_row_time_checkbox";
    public static final String PREF_TIME_TEXT_SIZE_EDITTEXT =
            "pref_time_display_size_edittext";
    public static final String PREF_UPDATE_TIME_LIST = "pref_update_time_list";
    public static final String PREF_MILLISECONDS_CHECKBOX =
            "pref_milliseconds_checkbox";
    public static final String PREF_SIGN_CHECKBOX = "pref_sign_checkbox";
    public static final String PREF_PUZZLETYPES_MULTISELECTLIST =
            "pref_puzzletypes_multiselectlist";

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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

            CheckBoxPreference inspection = (CheckBoxPreference) findPreference(
                    PREF_INSPECTION_CHECKBOX);
            inspection.setOnPreferenceChangeListener(new Preference
                    .OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference,
                                                  Object newValue) {
                    if (newValue.toString().equals("true")) {
                        CheckBoxPreference hold = (CheckBoxPreference)
                                findPreference(PREF_HOLDTOSTART_CHECKBOX);
                        hold.setChecked(true);
                    }
                    return true;
                }
            });

            EditTextPreference size = (EditTextPreference) findPreference(
                    PREF_TIME_TEXT_SIZE_EDITTEXT);
            size.setOnPreferenceChangeListener(new Preference
                    .OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference,
                                                  Object newValue) {
                    EditTextPreference size = (EditTextPreference) preference;
                    size.setSummary(newValue.toString());
                    return true;
                }
            });
            size.setSummary(size.getText());

            ListPreference updateOccurrence = (ListPreference) findPreference
                    (PREF_UPDATE_TIME_LIST);
            updateOccurrence.setEntryValues(new String[]{"0", "1", "2"});
            updateOccurrence.setOnPreferenceChangeListener(new Preference
                    .OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference,
                                                  Object newValue) {
                    ListPreference updateTime = (ListPreference) preference;
                    updateTime.setSummary(updateTime.getEntries()[Integer
                            .parseInt(newValue.toString())]);
                    return true;
                }
            });
            updateOccurrence.setSummary(updateOccurrence.getEntry());


            MultiSelectListPreference puzzleTypeMultiList =
                    (MultiSelectListPreference) findPreference
                            (PREF_PUZZLETYPES_MULTISELECTLIST);

            if (puzzleTypeMultiList.getValues().size() == 0) {
                Set<String> all = new HashSet<>();
                for (PuzzleType p : PuzzleType.values()) {
                    all.add(p.name());
                }
                puzzleTypeMultiList.setValues(all);
            }

            List<String> entries = new ArrayList<>();
            for (PuzzleType i : PuzzleType.values()) {
                String uiName = i.getUiName(getActivity());
                if (!i.official) {
                    uiName += " - " + getString(R.string.unofficial);
                }
                entries.add(uiName);
            }
            puzzleTypeMultiList.setEntries(entries.toArray(new
                    CharSequence[entries.size()]));

            List<String> entryValues = new ArrayList<>();
            for (PuzzleType p : PuzzleType.values()) {
                entryValues.add(p.name());
            }
            puzzleTypeMultiList.setEntryValues(entryValues.toArray(new
                    CharSequence[entryValues.size()]));

            puzzleTypeMultiList.setOnPreferenceChangeListener(new Preference
                    .OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference,
                                                  Object newValue) {
                    Set selected = (Set) newValue;
                    if (selected.size() == 0) {
                        Toast.makeText(getActivity(),
                                getString(R.string
                                        .no_disable_all_puzzletypes),
                                Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    for (PuzzleType p : PuzzleType.values()) {
                        PuzzleType.valueOf(p.name()).setEnabled(selected
                                .contains(p.name()));
                    }
                    return true;
                }
            });
        }
    }
}
