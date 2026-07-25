package io.github.outakeshi.indexsmith.sql.common;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 */
public enum CompareEnum {
    /**
     *
     */
    EQ("="),

    NE("!="),

    GT(">"),

    GTE(">="),

    LT("<"),

    LTE("<=");

    private final String text;

    private static final Map<String, CompareEnum> TEXT_TO_ENUM = Stream.of(CompareEnum.values())
            .collect(Collectors.toMap(o -> o.text, o -> o));

    CompareEnum(String text) {
        this.text = text;
    }

    public static void main(String[] args) {
        Map<String, CompareEnum> textToEnum = Stream.of(CompareEnum.values()).collect(Collectors.toMap(o -> o.text, o -> o));
    }

    public static CompareEnum textOfEnum(String text) {
        CompareEnum value = TEXT_TO_ENUM.get(text);
        if (value == null) {
            throw new IllegalArgumentException("Unsupported comparison operator: " + text);
        }
        return value;
    }
}
