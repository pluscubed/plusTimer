package com.pluscubed.plustimer.ui;


import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v14.preference.MultiSelectListPreference;
import android.support.v14.preference.PreferenceFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.ui.basedrawer.ThemableActivity;
import com.pluscubed.plustimer.utils.PrefUtils;
import com.pluscubed.plustimer.utils.Utils;

import java.io.IOException;
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
        public void onCreatePreferences(Bundle bundle, String s) {
            addPreferencesFromResource(R.xml.preferences);

            EditTextPreference size = (EditTextPreference) findPreference(PrefUtils.PREF_TIME_TEXT_SIZE_EDITTEXT);
            size.setOnPreferenceChangeListener((preference, newValue) -> {
                if (Integer.valueOf(newValue.toString()) > 500) {
                    Toast.makeText(getActivity(), getString(R.string.text_size_warning), Toast.LENGTH_SHORT)
                            .show();
                    return false;
                }
                EditTextPreference size1 = (EditTextPreference) preference;
                size1.setSummary(newValue.toString());
                return true;
            });
            size.setSummary(size.getText());

            EditTextPreference scrambleSize = (EditTextPreference)
                    findPreference(PrefUtils.PREF_SCRAMBLE_TEXT_SIZE_EDITTEXT);
            scrambleSize.setOnPreferenceChangeListener((preference, newValue) -> {
                if (Integer.valueOf(newValue.toString()) > 500) {
                    Toast.makeText(getActivity(), getString(R.string.text_size_warning),
                            Toast.LENGTH_SHORT)
                            .show();
                    return false;
                }
                EditTextPreference scrambleSize1 = (EditTextPreference) preference;
                scrambleSize1.setSummary(newValue.toString());
                return true;
            });
            scrambleSize.setSummary(scrambleSize.getText());

            ListPreference updateOccurrence = (ListPreference)
                    findPreference(PrefUtils.PREF_UPDATE_TIME_LIST);
            updateOccurrence.setEntryValues(new String[]{"0", "1", "2"});

            ListPreference theme = (ListPreference) findPreference(PrefUtils.PREF_THEME_LIST);
            theme.setEntryValues(new String[]{"0", "1", "2"});
            theme.setOnPreferenceChangeListener((preference, newValue) -> {
                getActivity().recreate();
                return true;
            });


            MultiSelectListPreference puzzleTypeMultiList =
                    (MultiSelectListPreference) findPreference(PrefUtils
                            .PREF_PUZZLETYPES_MULTISELECTLIST);

            if (puzzleTypeMultiList.getValues().size() == 0) {
                Set<String> all = new HashSet<>();
                for (PuzzleType p : PuzzleType.getPuzzleTypes()) {
                    all.add(p.getId());
                }
                puzzleTypeMultiList.setValues(all);
            }

            List<String> entries = new ArrayList<>();
            for (PuzzleType i : PuzzleType.getPuzzleTypes()) {
                String uiName = i.getName();
                if (!i.isScramblerOfficial()) {
                    uiName += " - " + getString(R.string.unofficial);
                }
                entries.add(uiName);
            }
            puzzleTypeMultiList.setEntries(entries.toArray(new
                    CharSequence[entries.size()]));

            List<String> entryValues = new ArrayList<>();
            for (PuzzleType p : PuzzleType.getPuzzleTypes()) {
                entryValues.add(p.getId());
            }
            puzzleTypeMultiList.setEntryValues(entryValues.toArray(new
                    CharSequence[entryValues.size()]));

            puzzleTypeMultiList.setOnPreferenceChangeListener((preference, newValue) -> {
                Set selected = (Set) newValue;
                if (selected.size() == 0) {
                    Toast.makeText(getActivity(),
                            getString(R.string
                                    .no_disable_all_puzzletypes),
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                for (PuzzleType p : PuzzleType.getPuzzleTypes()) {
                    try {
                        p.setEnabled(getActivity(), selected.contains(p.getId()));
                    } catch (CouchbaseLiteException | IOException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            });
        }

        @Override
        public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
            RecyclerView recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState);
            recyclerView.addItemDecoration(new DividerDecoration());

            return recyclerView;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            setDivider(null);
        }

        //From PreferenceFragment in v14 library - modified to not draw on first or last item
        private class DividerDecoration extends RecyclerView.ItemDecoration {
            private Drawable mDivider;
            private int mDividerHeight;

            private DividerDecoration() {
                mDivider = ContextCompat.getDrawable(getActivity(), R.drawable.preference_list_divider_material);
                mDividerHeight = Utils.convertDpToPx(getActivity(), 1);
            }

            public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
                if (this.mDivider != null) {
                    int childCount = parent.getChildCount();
                    int width = parent.getWidth();

                    for (int childViewIndex = 0; childViewIndex < childCount; ++childViewIndex) {
                        View view = parent.getChildAt(childViewIndex);
                        int top;
                        if (this.shouldDrawDividerAbove(view, parent)) {
                            top = (int) ViewCompat.getY(view);
                            this.mDivider.setBounds(0, top, width, top + this.mDividerHeight);
                            this.mDivider.draw(c);
                        }

                        if (this.shouldDrawDividerBelow(view, parent)) {
                            top = (int) ViewCompat.getY(view) + view.getHeight();
                            this.mDivider.setBounds(0, top, width, top + this.mDividerHeight);
                            this.mDivider.draw(c);
                        }
                    }

                }
            }

            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                if (this.shouldDrawDividerAbove(view, parent)) {
                    outRect.top = this.mDividerHeight;
                }

                if (this.shouldDrawDividerBelow(view, parent)) {
                    outRect.bottom = this.mDividerHeight;
                }

            }

            private boolean shouldDrawDividerAbove(View view, RecyclerView parent) {
                /*RecyclerView.ViewHolder holder = parent.getChildViewHolder(view);
                return holder.getAdapterPosition() == 0 && ((PreferenceViewHolder)holder).isDividerAllowedAbove();*/
                return false;
            }

            private boolean shouldDrawDividerBelow(View view, RecyclerView parent) {
                PreferenceViewHolder holder = (PreferenceViewHolder) parent.getChildViewHolder(view);
                boolean nextAllowed = true;
                int index = parent.indexOfChild(view);
                if (index < parent.getChildCount() - 1) {
                    View nextView = parent.getChildAt(index + 1);
                    PreferenceViewHolder nextHolder = (PreferenceViewHolder) parent.getChildViewHolder(nextView);
                    nextAllowed = nextHolder.isDividerAllowedAbove();
                }

                return holder.getAdapterPosition() != parent.getAdapter().getItemCount() - 1
                        && nextAllowed
                        && holder.isDividerAllowedBelow();
            }

            public void setDivider(Drawable divider) {
                if (divider != null) {
                    this.mDividerHeight = divider.getIntrinsicHeight();
                } else {
                    this.mDividerHeight = 0;
                }

                this.mDivider = divider;
                getListView().invalidateItemDecorations();
            }

            public void setDividerHeight(int dividerHeight) {
                this.mDividerHeight = dividerHeight;
                getListView().invalidateItemDecorations();
            }
        }
    }
}
