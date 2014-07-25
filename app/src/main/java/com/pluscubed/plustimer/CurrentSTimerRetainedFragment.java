package com.pluscubed.plustimer;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.Fragment;

import net.gnehzr.tnoodle.scrambles.InvalidScrambleException;

/**
 * Retained fragment alongside CurrentSTimerFragment
 */
public class CurrentSTimerRetainedFragment extends Fragment implements CurrentSTimerFragment.RetainedFragmentCallback {
    Handler mScramblerThreadHandler;
    HandlerThread mScramblerThread;
    private ScrambleAndSvg mCurrentScrambleAndSvg;
    private ScrambleAndSvg mNextScrambleAndSvg;

    private boolean mScrambling;

    private CurrentSTimerFragmentCallback mCallback;

    @Override
    public void setTimerFragmentCallback(CurrentSTimerFragmentCallback fragment) {
        mCallback = fragment;
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
                if (mCallback != null) {
                    mCallback.getUiHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            mCallback.updateScrambleTextAndImageToCurrent();
                            mCallback.enableOptionsMenu(true);
                        }
                    });
                }
            }
        });

    }

    //Generate scramble image and text
    private ScrambleAndSvg generateScramble() {

        String scramble = PuzzleType.get(PuzzleType.CURRENT).getPuzzle().generateScramble();
        ScrambleAndSvg scrambleAndSvg = null;
        try {
            scrambleAndSvg = new ScrambleAndSvg(scramble, PuzzleType.get(PuzzleType.CURRENT).getPuzzle().drawScramble(scramble, null));
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

    private void startScramblerThread() {
        mScramblerThread = new HandlerThread("ScramblerThread");
        mScramblerThread.start();
        mScramblerThreadHandler = new Handler(mScramblerThread.getLooper());
    }

    private void stopScramblerThread() {
        if (mScramblerThread != null) {
            mScramblerThread.quit();
            mScramblerThread = null;
            mScramblerThreadHandler = null;
        }
    }

    @Override
    public void resetScramblerThread() {
        stopScramblerThread();
        startScramblerThread();
        mScrambling = false;
        mCurrentScrambleAndSvg = null;
        mNextScrambleAndSvg = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        startScramblerThread();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopScramblerThread();
    }
}
