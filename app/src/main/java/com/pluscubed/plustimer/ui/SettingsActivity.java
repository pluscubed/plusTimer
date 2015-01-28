package com.pluscubed.plustimer.ui;


import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.utils.PrefUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Settings Activity and Fragment
 */
public class SettingsActivity extends ThemableActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_toolbar);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);

        FragmentManager fm = getFragmentManager();
        Fragment f = fm.findFragmentById(R.id
                .activity_with_toolbar_content_framelayout);
        if (f == null) {
            fm.beginTransaction()
                    .replace(R.id.activity_with_toolbar_content_framelayout,
                            new SettingsFragment())
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
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle paramBundle) {
            super.onCreate(paramBundle);
            addPreferencesFromResource(R.xml.preferences);

            EditTextPreference size = (EditTextPreference)
                    findPreference(PrefUtils.PREF_TIME_TEXT_SIZE_EDITTEXT);
            size.setOnPreferenceChangeListener(new Preference
                    .OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference,
                                                  Object newValue) {
                    if (Integer.valueOf(newValue.toString()) > 500) {
                        Toast.makeText(getActivity(), getString(R.string.text_size_warning), Toast.LENGTH_SHORT)
                                .show();
                        return false;
                    }
                    EditTextPreference size = (EditTextPreference) preference;
                    size.setSummary(newValue.toString());
                    return true;
                }
            });
            size.setSummary(size.getText());

            ListPreference updateOccurrence = (ListPreference)
                    findPreference(PrefUtils.PREF_UPDATE_TIME_LIST);
            updateOccurrence.setEntryValues(new String[]{"0", "1", "2"});

            ListPreference theme = (ListPreference) findPreference(PrefUtils.PREF_THEME_LIST);
            theme.setEntryValues(new String[]{"0", "1", "2"});
            theme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    getActivity().recreate();
                    return true;
                }
            });


            MultiSelectListPreference puzzleTypeMultiList =
                    (MultiSelectListPreference) findPreference(PrefUtils.PREF_PUZZLETYPES_MULTISELECTLIST);

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
