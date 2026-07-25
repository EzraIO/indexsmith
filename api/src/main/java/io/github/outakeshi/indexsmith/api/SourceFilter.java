package io.github.outakeshi.indexsmith.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Source fields to include or exclude from search hits. */
public final class SourceFilter {
    private final List<String> includes;
    private final List<String> excludes;

    public SourceFilter(Collection<String> includes, Collection<String> excludes) {
        this.includes = immutableFields(includes);
        this.excludes = immutableFields(excludes);
        if (this.includes.isEmpty() && this.excludes.isEmpty()) {
            throw new IllegalArgumentException("At least one source include or exclude is required");
        }
    }

    public List<String> getIncludes() { return includes; }
    public List<String> getExcludes() { return excludes; }

    private static List<String> immutableFields(Collection<String> fields) {
        List<String> result = new ArrayList<>();
        if (fields != null) {
            for (String field : fields) {
                if (field == null || field.trim().isEmpty()) {
                    throw new IllegalArgumentException("source field must not be blank");
                }
                result.add(field.trim());
            }
        }
        return Collections.unmodifiableList(result);
    }
}
