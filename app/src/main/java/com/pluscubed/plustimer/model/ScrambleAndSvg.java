package com.pluscubed.plustimer.model;

import android.content.Context;
import android.support.annotation.Nullable;

import com.pluscubed.plustimer.utils.Utils;

/**
 * Scramble and Svg
 */

public class ScrambleAndSvg {

    @Nullable
    private String mSvg;
    private String mScramble;

    public ScrambleAndSvg(String scramble, @Nullable String svg) {
        mScramble = scramble;
        mSvg = svg;
    }

    public String getScramble() {
        return mScramble;
    }

    public void setScramble(Context context, String scramble, String puzzleTypeName) {
        //TODO: Evaluate performance
        mScramble = Utils.signToWcaNotation(context, scramble, puzzleTypeName).toBlocking().value();
    }

    public String getSvg() {
        return mSvg;
    }

}

