package com.pluscubed.plustimer.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.pluscubed.plustimer.Util;

import java.lang.reflect.Type;

/**
 * Scramble and Svg
 */

public class ScrambleAndSvg {

    private String mSvg;
    private String mScramble;

    public ScrambleAndSvg(String scramble, String svg) {
        mScramble = scramble;
        mSvg = svg;
    }

    public String getUiScramble(boolean sign, String puzzleTypeName) {
        return sign ? Util.wcaToSignNotation(mScramble, puzzleTypeName) : mScramble;
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

    public static class Serializer implements JsonSerializer<ScrambleAndSvg> {

        @Override
        public JsonElement serialize(ScrambleAndSvg src, Type typeOfSrc,
                                     JsonSerializationContext context) {
            return new JsonPrimitive(src.getScramble());
        }
    }

    public static class Deserializer implements JsonDeserializer<ScrambleAndSvg> {

        @Override
        public ScrambleAndSvg deserialize(JsonElement json, Type typeOfT,
                                          JsonDeserializationContext context) throws JsonParseException {
            return new ScrambleAndSvg(json.getAsJsonPrimitive().getAsString(), null);
        }
    }
}

