package it.philipkrauss.proxysystem.utils.regex;

import com.google.common.collect.Maps;

import java.util.Map;

public class RegexGenerator {

    public static RegexGenerator create() {
        return new RegexGenerator();
    }

    private final Map<String, String> REGEX_CACHE = Maps.newHashMap();

    private RegexGenerator() {

    }

    public String get(String string) {
        return REGEX_CACHE.getOrDefault(string, this.generate(string));
    }

    private String generate(String string) {
        StringBuilder regexBuilder = new StringBuilder();
        for (int index = 0; index < string.length(); index++) {
            char character = string.charAt(index);
            String characterString = String.valueOf(character);
            if (characterString.equals(" ")) regexBuilder.append("(%SPACE%+)+(\\W|_)*");
            regexBuilder.append((characterString.equals("#") ? "#" : "(" + character + ")+(\\W|_)*"));
        }
        String regexString = toLeetSpeak(regexBuilder.toString().replaceAll("\\[|\\]|,|\\s", "").replaceAll("%SPACE%", " "));
        if (!regexString.contains("#")) {
            regexString = "(" + regexString + ")";
        } else if (regexString.startsWith("#") && regexString.endsWith("#")) {
            regexString = regexString.replace("#", "");
            regexString = "\\b(" + regexString + ")\\b";
        } else if (regexString.startsWith("#")) {
            regexString = regexString.replace("#", "\\b(");
            regexString = regexString + ")";
        } else if (regexString.endsWith("#")) {
            regexString = regexString.replace("#", ")\\b");
            regexString = "(" + regexString;
        }
        REGEX_CACHE.put(string, regexString);
        return regexString;
    }

    String toLeetSpeak(String speak) {
        StringBuilder stringBuilder = new StringBuilder(speak.length());
        for (char character : speak.toCharArray()) {
            switch (character) {
                case 'a' -> stringBuilder.append("@|a|4");
                case 'b' -> stringBuilder.append("8|b");
                case 'c' -> stringBuilder.append("\\(|c");
                case 'd' -> stringBuilder.append("d");
                case 'e' -> stringBuilder.append("3|e");
                case 'f' -> stringBuilder.append("f");
                case 'g' -> stringBuilder.append("6|g");
                case 'h' -> stringBuilder.append("h");
                case 'i' -> stringBuilder.append("!|i|1");
                case 'j' -> stringBuilder.append("j");
                case 'k' -> stringBuilder.append("k");
                case 'l' -> stringBuilder.append("1|l");
                case 'm' -> stringBuilder.append("m");
                case 'n' -> stringBuilder.append("n");
                case 'o' -> stringBuilder.append("0|o");
                case 'p' -> stringBuilder.append("p");
                case 'q' -> stringBuilder.append("q");
                case 'r' -> stringBuilder.append("r");
                case 's' -> stringBuilder.append("\\$|s|5");
                case 't' -> stringBuilder.append("7|t");
                case 'u' -> stringBuilder.append("u");
                case 'v' -> stringBuilder.append("v");
                case 'w' -> stringBuilder.append("w");
                case 'x' -> stringBuilder.append("x");
                case 'y' -> stringBuilder.append("y");
                case 'z' -> stringBuilder.append("2|z");
                default -> stringBuilder.append(character);
            }
        }
        return stringBuilder.toString();
    }

}
