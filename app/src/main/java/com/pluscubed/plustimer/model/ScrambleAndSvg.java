package com.pluscubed.plustimer.model;

import com.pluscubed.plustimer.utils.Util;

/**
 * Scramble and Svg
 */

public class ScrambleAndSvg {

    private transient String mSvg;
    private String mScramble;

    public ScrambleAndSvg(String scramble, String svg) {
        mScramble = scramble;
        mSvg = svg;
    }

    public String getUiScramble(boolean sign, String puzzleTypeName) {
        return sign ? Util.wcaToSignNotation(mScramble,
                puzzleTypeName) : mScramble;
    }

    public String getScramble() {
        return mScramble;
    }

    public void setScramble(String scramble, String puzzleTypeName) {
        mScramble = Util.signToWcaNotation(scramble, puzzleTypeName);
    }

    public String getSvg() {
        return mSvg;
    }

}

