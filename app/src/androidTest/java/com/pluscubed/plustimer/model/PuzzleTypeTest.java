package com.pluscubed.plustimer.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.pluscubed.plustimer.App;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;


@RunWith(AndroidJUnit4.class)
public class PuzzleTypeTest extends InstrumentationTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
    }

    @Test
    public void initialize_firstRunPreCouchbase() throws CouchbaseLiteException, IOException {
        Context context = InstrumentationRegistry.getTargetContext();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putInt("pref_version_code", 23).apply();

        PuzzleType.initialize(context);

        Query puzzleTypesQuery = App.getDatabase(context).getView("puzzletypes").createQuery();
        QueryEnumerator rows = puzzleTypesQuery.run();
        for (QueryRow row : rows) {
            Log.d("Test", row.getDocument().getUserProperties().toString());

            //Actual stuff to test?
        }
    }
}
