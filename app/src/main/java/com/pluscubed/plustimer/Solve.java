package com.pluscubed.plustimer;

/**
 * solve times data object
 */
public class Solve {
    private ScrambleAndSvg scrambleAndSvg;
    private long time;

    public Solve(ScrambleAndSvg scramble, long time) {
        this.scrambleAndSvg = scramble;
        this.time = time;
    }

    public ScrambleAndSvg getScrambleAndSvg() {
        return scrambleAndSvg;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }


}
