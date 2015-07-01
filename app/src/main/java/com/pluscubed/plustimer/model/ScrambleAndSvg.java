package com.pluscubed.plustimer.model;

import android.support.annotation.Nullable;

import com.pluscubed.plustimer.utils.Utils;

/**
 * Scramble and Svg
 */

public class ScrambleAndSvg {

    @Nullable
    private transient String mSvg;
    private String mScramble;

    public ScrambleAndSvg(String scramble, @Nullable String svg) {
        mScramble = scramble;
        mSvg = svg;
    }

    public String getUiScramble(boolean sign, String puzzleTypeName) {
        return sign ? Utils.wcaToSignNotation(mScramble,
                puzzleTypeName) : mScramble;
    }

    public String getScramble() {
        return mScramble;
    }

    public void setScramble(String scramble, String puzzleTypeName) {
        mScramble = Utils.signToWcaNotation(scramble, puzzleTypeName);
    }

    public String getSvg() {
        return mSvg;
    }

}

