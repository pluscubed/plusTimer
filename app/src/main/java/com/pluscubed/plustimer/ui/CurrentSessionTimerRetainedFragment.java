package com.pluscubed.plustimer.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;

import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.ScrambleAndSvg;

import net.gnehzr.tnoodle.scrambles.InvalidScrambleException;

/**
 * Retained fragment alongside CurrentSTimerFragment
 */
public class CurrentSessionTimerRetainedFragment extends Fragment {

    private static final String STATE_CURRENT_SCRAMBLE = "current_scramble";

    private static final String STATE_CURRENT_SVG = "current_svg";

    private static final String STATE_NEXT_SCRAMBLE = "next_scramble";

    private static final String STATE_NEXT_SVG = "next_svg";

    private static final String STATE_SCRAMBLING = "scrambling";

    private Handler mScramblerThreadHandler;

    private HandlerThread mScramblerThread;

    private ScrambleAndSvg mCurrentScrambleAndSvg;

    private ScrambleAndSvg mNextScrambleAndSvg;

    private boolean mScrambling;

    private Callback mCallback;

    public void setTimerFragmentCallback(Callback fragment) {
        mCallback = fragment;
    }

    public boolean isScrambling() {
        return mScrambling;
    }

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
                            mCallback.enableMenuItems(true);
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
            scrambleAndSvg = new ScrambleAndSvg(scramble,
                    PuzzleType.get(PuzzleType.CURRENT).getPuzzle().drawScramble(scramble, null)
                            .toString());
        } catch (InvalidScrambleException e) {
            e.printStackTrace();
        }

        return scrambleAndSvg;
    }

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

        if (savedInstanceState != null) {
            String current_scramble = savedInstanceState.getString(STATE_CURRENT_SCRAMBLE);
            if (current_scramble != null) {
                mCurrentScrambleAndSvg = new ScrambleAndSvg(current_scramble,
                        savedInstanceState.getString(STATE_CURRENT_SVG));
            }
            String next_scramble = savedInstanceState.getString(STATE_NEXT_SCRAMBLE);
            if (next_scramble != null) {
                mNextScrambleAndSvg = new ScrambleAndSvg(next_scramble,
                        savedInstanceState.getString(STATE_NEXT_SVG));
            }
            mScrambling = savedInstanceState.getBoolean(STATE_SCRAMBLING);
            if (mScrambling) {
                generateNextScramble();
                updateViews();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopScramblerThread();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mCurrentScrambleAndSvg != null) {
            outState.putString(STATE_CURRENT_SCRAMBLE, mCurrentScrambleAndSvg.scramble);
            outState.putString(STATE_CURRENT_SVG, mCurrentScrambleAndSvg.svg);
        }
        if (mNextScrambleAndSvg != null) {
            outState.putString(STATE_NEXT_SCRAMBLE, mNextScrambleAndSvg.scramble);
            outState.putString(STATE_NEXT_SVG, mNextScrambleAndSvg.svg);
        }
        outState.putBoolean(STATE_SCRAMBLING, mScrambling);
    }

    public interface Callback {

        Handler getUiHandler();

        void updateScrambleTextAndImageToCurrent();

        void enableMenuItems(boolean enable);
    }
}
