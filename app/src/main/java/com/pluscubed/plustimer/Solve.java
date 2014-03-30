package com.pluscubed.plustimer;

/**
 * solve times data object
 */
public class Solve {
    private String scramble;
    private long time;

    public Solve(String scramble, long time) {
        this.scramble = scramble;
        this.time = time;
    }

    public String getScramble() {
        return scramble;
    }

    public void setScramble(String scramble) {
        this.scramble = scramble;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

}
