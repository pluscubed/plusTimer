package com.pluscubed.plustimer.ui;


import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;

/**
 * Settings Activity and Fragment
 */
public class SettingsActivity extends ActionBarActivity {

    public static final String PREF_INSPECTION_CHECKBOX = "pref_inspection_checkbox";
    public static final String PREF_HOLDTOSTART_CHECKBOX = "pref_holdtostart_checkbox";
    public static final String PREF_KEEPSCREENON_CHECKBOX = "pref_keepscreenon_checkbox";
    public static final String PREF_TWO_ROW_TIME_CHECKBOX = "pref_two_row_time_checkbox";
    public static final String PREF_TIME_TEXT_SIZE_EDITTEXT = "pref_time_display_size_edittext";
    public static final String PREF_UPDATE_TIME_LIST = "pref_update_time_list";
    public static final String PREF_MILLISECONDS_CHECKBOX = "pref_milliseconds_checkbox";
    public static final String PREF_SIGN_CHECKBOX = "pref_sign_checkbox";
    public static final String PREFSCREEN_PUZZLETYPES = "pref_puzzletypes";
    public static final String PREF_PUZZLETYPE_ENABLE_PREFIX = "pref_puzzletype_enable_";

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
        getSupportActionBar().setHomeButtonEnabled(true);
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

        /**
         * Sets up the action bar for an {@link PreferenceScreen}
         */
        public static void initializeActionBar(PreferenceScreen preferenceScreen) {
            final Dialog dialog = preferenceScreen.getDialog();
            if (dialog != null) {
                dialog.getActionBar().setDisplayHomeAsUpEnabled(true);
                View homeBtn = dialog.findViewById(android.R.id.home);
                if (homeBtn != null) {
                    View.OnClickListener dismissDialogClickListener = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    };

                    ViewParent homeBtnContainer = homeBtn.getParent();

                    if (homeBtnContainer instanceof FrameLayout) {
                        ViewGroup containerParent = (ViewGroup) homeBtnContainer.getParent();

                        if (containerParent instanceof LinearLayout) {
                            containerParent.setOnClickListener(dismissDialogClickListener);
                        } else {
                            ((FrameLayout) homeBtnContainer).setOnClickListener(dismissDialogClickListener);
                        }
                    } else {
                        homeBtn.setOnClickListener(dismissDialogClickListener);
                    }
                }
            }
        }

        //Solution to setting up an action bar for a PreferenceScreen: http://stackoverflow.com/questions/16374820/action-bar-home-button-not-functional-with-nested-preferencescreen
        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, @NonNull Preference preference) {
            super.onPreferenceTreeClick(preferenceScreen, preference);

            // If the user has clicked on a preference screen, set up the action bar
            if (preference instanceof PreferenceScreen) {
                initializeActionBar((PreferenceScreen) preference);
            }

            return false;
        }

        @Override
        public void onCreate(Bundle paramBundle) {
            super.onCreate(paramBundle);
            addPreferencesFromResource(R.xml.preferences);
            CheckBoxPreference inspection = (CheckBoxPreference) findPreference(
                    PREF_INSPECTION_CHECKBOX);
            inspection.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue.toString().equals("true")) {
                        CheckBoxPreference hold = (CheckBoxPreference) findPreference(PREF_HOLDTOSTART_CHECKBOX);
                        hold.setChecked(true);
                    }
                    return true;
                }
            });
            EditTextPreference size = (EditTextPreference) findPreference(
                    PREF_TIME_TEXT_SIZE_EDITTEXT);
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
            PreferenceScreen puzzleTypeSetupScreen = (PreferenceScreen) findPreference(PREFSCREEN_PUZZLETYPES);
            for (PuzzleType i : PuzzleType.values()) {
                CheckBoxPreference puzzleTypeCheckBox = new CheckBoxPreference(getActivity());
                String uiName = i.getUiName(getActivity());
                if (!i.official) {
                    uiName += " - " + getString(R.string.unofficial);
                }
                puzzleTypeCheckBox.setTitle(uiName);
                puzzleTypeCheckBox.setDefaultValue(true);
                puzzleTypeCheckBox.setChecked(i.isEnabled());
                puzzleTypeCheckBox.setKey(PREF_PUZZLETYPE_ENABLE_PREFIX + i.name().toLowerCase());
                puzzleTypeCheckBox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (newValue.toString().equals("false")) {
                            int numberEnabled = 0;
                            for (PuzzleType i : PuzzleType.values()) {
                                if (preference.getSharedPreferences().getBoolean(PREF_PUZZLETYPE_ENABLE_PREFIX + i.name().toLowerCase(), true)) {
                                    numberEnabled++;
                                }
                            }
                            if (numberEnabled <= 1) {
                                Toast.makeText(getActivity(), getString(R.string.no_disable_all_puzzletypes), Toast.LENGTH_SHORT).show();
                                ((CheckBoxPreference) preference).setChecked(true);
                                return false;
                            }
                        }
                        PuzzleType.valueOf(preference.getKey().replace(PREF_PUZZLETYPE_ENABLE_PREFIX, "").toUpperCase()).setEnabled(newValue.toString().equals("true"));
                        return true;
                    }
                });
                puzzleTypeSetupScreen.addPreference(puzzleTypeCheckBox);
            }
        }
    }
}
