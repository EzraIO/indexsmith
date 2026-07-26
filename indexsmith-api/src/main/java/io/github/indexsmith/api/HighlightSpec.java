package io.github.indexsmith.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Highlight fields and common fragment options. */
public final class HighlightSpec {
    private final List<String> fields;
    private final List<String> preTags;
    private final List<String> postTags;
    private final Integer fragmentSize;
    private final Integer numberOfFragments;

    private HighlightSpec(Builder builder) {
        if (builder.fields.isEmpty()) throw new IllegalArgumentException("At least one highlight field is required");
        if (builder.fragmentSize != null && builder.fragmentSize <= 0) {
            throw new IllegalArgumentException("fragmentSize must be positive");
        }
        if (builder.numberOfFragments != null && builder.numberOfFragments < 0) {
            throw new IllegalArgumentException("numberOfFragments must not be negative");
        }
        if (builder.preTags.size() != builder.postTags.size()) {
            throw new IllegalArgumentException("preTags and postTags must have the same size");
        }
        this.fields = Collections.unmodifiableList(new ArrayList<>(builder.fields));
        this.preTags = Collections.unmodifiableList(new ArrayList<>(builder.preTags));
        this.postTags = Collections.unmodifiableList(new ArrayList<>(builder.postTags));
        this.fragmentSize = builder.fragmentSize;
        this.numberOfFragments = builder.numberOfFragments;
    }

    public static Builder builder() { return new Builder(); }
    public List<String> getFields() { return fields; }
    public List<String> getPreTags() { return preTags; }
    public List<String> getPostTags() { return postTags; }
    public Integer getFragmentSize() { return fragmentSize; }
    public Integer getNumberOfFragments() { return numberOfFragments; }

    public static final class Builder {
        private final List<String> fields = new ArrayList<>();
        private final List<String> preTags = new ArrayList<>();
        private final List<String> postTags = new ArrayList<>();
        private Integer fragmentSize;
        private Integer numberOfFragments;

        public Builder field(String field) {
            if (field == null || field.trim().isEmpty()) throw new IllegalArgumentException("highlight field must not be blank");
            fields.add(field.trim());
            return this;
        }
        public Builder field(QueryField field) { return field(field.getEsFieldPathName()); }
        public Builder tags(String preTag, String postTag) {
            if (preTag == null || postTag == null) throw new IllegalArgumentException("highlight tags must not be null");
            preTags.add(preTag);
            postTags.add(postTag);
            return this;
        }
        public Builder fragmentSize(int value) { fragmentSize = value; return this; }
        public Builder numberOfFragments(int value) { numberOfFragments = value; return this; }
        public HighlightSpec build() { return new HighlightSpec(this); }
    }
}
