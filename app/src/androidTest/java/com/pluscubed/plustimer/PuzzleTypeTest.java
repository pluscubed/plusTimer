package com.pluscubed.plustimer;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.pluscubed.plustimer.model.PuzzleType;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PuzzleTypeTest {

    @Test
    public void testInitialize_firstSetup() {
        //PrefUtils.saveVersionCode(InstrumentationRegistry.getContext());
        PuzzleType.initialize(InstrumentationRegistry.getContext());
    }
}
