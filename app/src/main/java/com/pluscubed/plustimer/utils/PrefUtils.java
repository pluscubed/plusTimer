package com.pluscubed.plustimer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.pluscubed.plustimer.BuildConfig;
import com.pluscubed.plustimer.model.PuzzleType;

import java.util.HashSet;
import java.util.Set;

public class PrefUtils {
    public static final String PREF_INSPECTION_CHECKBOX =
            "pref_inspection_checkbox";
    public static final String PREF_HOLDTOSTART_CHECKBOX =
            "pref_holdtostart_checkbox";
    public static final String PREF_KEEPSCREENON_CHECKBOX =
            "pref_keepscreenon_checkbox";
    public static final String PREF_TWO_ROW_TIME_CHECKBOX =
            "pref_two_row_time_checkbox";
    public static final String PREF_TIME_TEXT_SIZE_EDITTEXT =
            "pref_time_display_size_edittext";
    public static final String PREF_UPDATE_TIME_LIST =
            "pref_update_time_list";
    public static final String PREF_MILLISECONDS_CHECKBOX =
            "pref_milliseconds_checkbox";
    public static final String PREF_SIGN_CHECKBOX =
            "pref_sign_checkbox";
    public static final String PREF_PUZZLETYPES_MULTISELECTLIST =
            "pref_puzzletypes_multiselectlist";
    public static final String PREF_THEME_LIST =
            "pref_theme_list";
    public static final String PREF_MONOSPACE_SCRAMBLES_CHECKBOX =
            "pref_monospace_scrambles_checkbox";
    public static final String PREF_VERSION_CODE =
            "pref_version_code";
    public static final String PREF_CURRENT_PUZZLETYPE =
            "current_puzzletype";
    private static final String PREF_WELCOME_DONE =
            "welcome_done";

    public static boolean isInspectionEnabled(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_INSPECTION_CHECKBOX, true);
    }

    public static boolean isHoldToStartEnabled(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_HOLDTOSTART_CHECKBOX, true);
    }

    public static boolean isTwoRowTimeEnabled(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_TWO_ROW_TIME_CHECKBOX, true);
    }

    public static boolean isDisplayMillisecondsEnabled(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_MILLISECONDS_CHECKBOX, true);
    }

    public static boolean isKeepScreenOnEnabled(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_KEEPSCREENON_CHECKBOX, true);
    }

    public static boolean isSignEnabled(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_SIGN_CHECKBOX, true);
    }

    public static boolean isMonospaceScrambleFontEnabled(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_MONOSPACE_SCRAMBLES_CHECKBOX, false);
    }

    public static boolean isWelcomeDone(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_WELCOME_DONE, false);
    }

    public static void markWelcomeDone(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_WELCOME_DONE, true).apply();
    }

    public static Set<String> getSelectedPuzzleTypeNames(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getStringSet(PREF_PUZZLETYPES_MULTISELECTLIST,
                new HashSet<String>());
    }

    public static int getTimerTextSize(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(sp.getString(PREF_TIME_TEXT_SIZE_EDITTEXT, "100"));
    }

    public static void saveVersionCode(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt(PrefUtils.PREF_VERSION_CODE, BuildConfig.VERSION_CODE).apply();
    }

    public static int getVersionCode(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt(PREF_VERSION_CODE, 10);
    }

    public static PuzzleType getCurrentPuzzleType(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return PuzzleType.valueOf(sp.getString(
                PrefUtils.PREF_CURRENT_PUZZLETYPE, PuzzleType.THREE.name()
        ));
    }

    public static void saveCurrentPuzzleType(final Context context, String name) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putString(PrefUtils.PREF_CURRENT_PUZZLETYPE, name).apply();
    }

    public static TimerUpdate getTimerUpdateMode(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        switch (Integer.parseInt(sp.getString(PREF_UPDATE_TIME_LIST, "0"))) {
            case 0:
                return TimerUpdate.ON;
            case 1:
                return TimerUpdate.SECONDS;
            case 2:
                return TimerUpdate.OFF;
            default:
                return TimerUpdate.ON;
        }
    }

    public static Theme getTheme(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        switch (Integer.parseInt(sp.getString(PREF_THEME_LIST, "0"))) {
            case 0:
                return Theme.LIGHT;
            case 1:
                return Theme.DARK;
            case 2:
                return Theme.BLACK;
            default:
                return Theme.LIGHT;
        }
    }

    public enum Theme {
        LIGHT, DARK, BLACK
    }

    public enum TimerUpdate {
        ON, SECONDS, OFF
    }


}
