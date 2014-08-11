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
            scrambleAndSvg = new ScrambleAndSvg(scramble, PuzzleType.get(PuzzleType.CURRENT).getPuzzle().drawScramble(scramble, null));
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopScramblerThread();
    }

    public interface Callback {
        Handler getUiHandler();

        void updateScrambleTextAndImageToCurrent();

        void enableMenuItems(boolean enable);
    }
}
