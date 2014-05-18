package com.pluscubed.plustimer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Main Activity
 */
public class MainActivity extends ActionBarActivity {
    private String[] mDrawerTitles;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDrawerTitles = getResources().getStringArray(R.array.drawer_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerListView = (ListView) findViewById(R.id.drawer_listview);

        mDrawerListView.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mDrawerTitles));
        mDrawerListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        Fragment fragment = new CurrentSessionFragment();

                        // Insert the fragment by replacing any existing fragment
                        FragmentManager fragmentManager = getSupportFragmentManager();
                        fragmentManager.beginTransaction()
                                .replace(R.id.content_frame, fragment)
                                .commit();

                        // Highlight the selected item, update the title, and close the drawer
                        mDrawerListView.setItemChecked(position, true);
                        setTitle(mDrawerTitles[position]);
                        mDrawerLayout.closeDrawer(mDrawerListView);
                        break;
                }
            }
        });
        selectItem(0);
    }

    void selectItem(int pos) {
        switch (pos) {
            case 0:
                Fragment fragment = new CurrentSessionFragment();

                // Insert the fragment by replacing any existing fragment
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, fragment)
                        .commit();

                // Highlight the selected item, update the title, and close the drawer
                mDrawerListView.setItemChecked(pos, true);
                setTitle(mDrawerTitles[pos]);
                mDrawerLayout.closeDrawer(mDrawerListView);
                break;
        }
    }
}
