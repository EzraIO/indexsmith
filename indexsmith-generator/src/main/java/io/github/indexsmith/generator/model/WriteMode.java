package io.github.indexsmith.generator.model;

/** Behavior when generated content differs from an existing source file. */
public enum WriteMode {
    FAIL,
    SKIP,
    OVERWRITE;

    public static WriteMode parse(String value) {
        if (value == null || value.trim().isEmpty()) return FAIL;
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown generator writeMode '" + value
                    + "'. Expected FAIL, SKIP, or OVERWRITE", exception);
        }
    }
}
