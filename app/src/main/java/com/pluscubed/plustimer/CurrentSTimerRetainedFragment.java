package com.pluscubed.plustimer;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.Fragment;

import net.gnehzr.tnoodle.scrambles.InvalidScrambleException;

/**
 * Created by dc on 7/6/2014.
 */
public class CurrentSTimerRetainedFragment extends Fragment implements CurrentSTimerFragment.RetainedFragmentCallback {
    Handler mScramblerThreadHandler;
    HandlerThread mScramblerThread;
    private ScrambleAndSvg mCurrentScrambleAndSvg;
    private ScrambleAndSvg mNextScrambleAndSvg;

    private boolean mScrambling;

    private CurrentSTimerFragment mCurrentSTimerFragment;

    @Override
    public void setTimerFragmentCallback(CurrentSTimerFragment fragment) {
        mCurrentSTimerFragment = fragment;
    }

    @Override
    public boolean isScrambling() {
        return mScrambling;
    }

    @Override
    public void generateNextScramble() {
        postToScrambleThread(new Runnable() {
            @Override
            public void run() {
                mScrambling = true;
                mNextScrambleAndSvg = generateScramble();
                mScrambling = false;

            }
        });

    }

    @Override
    public void updateViews() {
        postToScrambleThread(new Runnable() {
            @Override
            public void run() {
                mCurrentScrambleAndSvg = mNextScrambleAndSvg;
                mNextScrambleAndSvg = null;
                if (mCurrentSTimerFragment != null) {
                    mCurrentSTimerFragment.getUiHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            mCurrentSTimerFragment.updateScrambleTextAndImageToCurrent();
                            mCurrentSTimerFragment.enableOptionsMenu(true);
                        }
                    });
                }
            }
        });

    }

    //Generate scramble image and text
    private ScrambleAndSvg generateScramble() {

        String scramble = PuzzleType.sCurrentPuzzleType.getPuzzle().generateScramble();
        ScrambleAndSvg scrambleAndSvg = null;
        try {
            scrambleAndSvg = new ScrambleAndSvg(scramble, PuzzleType.sCurrentPuzzleType.getPuzzle().drawScramble(scramble, null));
        } catch (InvalidScrambleException e) {
            e.printStackTrace();
        }

        return scrambleAndSvg;
    }

    @Override
    public ScrambleAndSvg getCurrentScrambleAndSvg() {
        return mCurrentScrambleAndSvg;
    }


    private void postToScrambleThread(Runnable r) {

        mScramblerThreadHandler.post(r);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mScramblerThread = new HandlerThread("ScramblerThread");
        mScramblerThread.start();
        mScramblerThreadHandler = new Handler(mScramblerThread.getLooper());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mScramblerThread != null) {
            mScramblerThread.quit();
            mScramblerThread = null;
            mScramblerThreadHandler = null;
        }
    }
}
