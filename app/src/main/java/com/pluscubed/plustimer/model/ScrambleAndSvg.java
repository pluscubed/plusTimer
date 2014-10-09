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

    public String svg;
    private String mScramble;

    public ScrambleAndSvg(String scramble, String svg) {
        this.mScramble = scramble;
        this.svg = svg;
    }

    public String getScramble(boolean sign) {
        return sign ? Util.wcaToSignNotation(mScramble) : mScramble;
    }

    public void setScramble(String scramble) {
        mScramble = Util.signToWcaNotation(scramble);
    }

    public static class Serializer implements JsonSerializer<ScrambleAndSvg> {

        @Override
        public JsonElement serialize(ScrambleAndSvg src, Type typeOfSrc,
                                     JsonSerializationContext context) {
            return new JsonPrimitive(src.mScramble);
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

