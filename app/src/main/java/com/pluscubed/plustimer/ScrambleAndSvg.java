package com.pluscubed.plustimer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import net.gnehzr.tnoodle.svglite.Svg;

import java.lang.reflect.Type;

/**
 * Scramble and Svg
 */

public class ScrambleAndSvg {
    public String scramble;
    public Svg svgLite;

    public ScrambleAndSvg(String scramble, Svg svgLite) {
        this.scramble = scramble;
        this.svgLite = svgLite;
    }

    public static class Serializer implements JsonSerializer<ScrambleAndSvg> {
        @Override
        public JsonElement serialize(ScrambleAndSvg src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.scramble);
        }
    }

    public static class Deserializer implements JsonDeserializer<ScrambleAndSvg> {
        @Override
        public ScrambleAndSvg deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return new ScrambleAndSvg(json.getAsJsonPrimitive().getAsString(), null);
        }
    }
}

