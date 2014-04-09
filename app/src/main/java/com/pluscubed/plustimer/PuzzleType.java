package com.pluscubed.plustimer;

import net.gnehzr.tnoodle.scrambles.Puzzle;
import net.gnehzr.tnoodle.scrambles.PuzzlePlugins;
import net.gnehzr.tnoodle.utils.BadLazyClassDescriptionException;
import net.gnehzr.tnoodle.utils.LazyInstantiator;
import net.gnehzr.tnoodle.utils.LazyInstantiatorException;

import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Puzzle type enum
 */
public enum PuzzleType {
    //TODO: find faster square-1 scrambler
    //SQ1("sq1", "Square 1"),
    SKEWB("skewb", "Skewb"),
    PYRAMINX("pyram", "Pyraminx"),
    MINX("minx", "Megaminx"),
    CLOCK("clock", "Clock"),
    SEVEN("777", "7x7"),
    SIX("666", "6x6"),
    FIVE("555", "5x5"),
    FOURFAST("444fast", "4x4-fast"),
    THREE("333", "3x3"),
    TWO("222", "2x2");


    public final String scramblerSpec;
    public final String displayName;
    private Session session;
    private Puzzle puzzle;


    PuzzleType(String scramblerSpec, String displayName) {
        this.scramblerSpec = scramblerSpec;
        this.displayName = displayName;
        session = new Session();
    }

    public Puzzle getPuzzle() {
        if (puzzle == null) {
            try {
                puzzle = PuzzlePlugins.getScramblers().get(scramblerSpec).cachedInstance();
            } catch (LazyInstantiatorException e) {
                e.printStackTrace();
            } catch (BadLazyClassDescriptionException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return puzzle;
    }



    @Override
    public String toString() {
        return displayName;
    }

    public Session getSession(){
        if(session==null)
            session=new Session();
        return session;
    }

    public void resetSession(){
        session=null;
    }

}
