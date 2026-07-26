package io.github.indexsmith.client;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EsVersion implements Comparable<EsVersion> {
    private static final Pattern VERSION = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?.*$");

    private final int major;
    private final int minor;
    private final int patch;

    public EsVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public static EsVersion parse(String value) {
        Matcher matcher = VERSION.matcher(Objects.requireNonNull(value, "version").trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid Elasticsearch version: " + value);
        }
        return new EsVersion(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3)));
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    @Override
    public int compareTo(EsVersion other) {
        Objects.requireNonNull(other, "other");
        int result = Integer.compare(major, other.major);
        if (result == 0) result = Integer.compare(minor, other.minor);
        if (result == 0) result = Integer.compare(patch, other.patch);
        return result;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }

    @Override
    public boolean equals(Object value) {
        if (this == value) return true;
        if (!(value instanceof EsVersion)) return false;
        EsVersion other = (EsVersion) value;
        return major == other.major && minor == other.minor && patch == other.patch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }
}
