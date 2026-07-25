package io.github.outakeshi.indexsmith.generator.builder;

import com.github.javaparser.ast.comments.BlockComment;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 */
public class BlockCommentBuilder {

    public static Builder builder(){
        return new Builder();
    }

    public static Builder builder(List<String> comments){
        return new Builder(comments);
    }

    public static class Builder {
        private final List<String> comments = new ArrayList<>();

        public Builder() {

        }

        public Builder(List<String> comments) {
            this.comments.addAll(comments);
        }

        public Builder addComment(String comment) {
            this.comments.add(comment);
            return this;
        }

        public Builder addComments(List<String> comments) {
            this.comments.addAll(comments);
            return this;
        }

        public BlockComment build() {
            if (this.comments.isEmpty()) {
                return new BlockComment();
            }
            List<String> formateComments = comments.stream().map(o -> " * " + o).collect(Collectors.toList());
            String commentBlock = StringUtils.join(formateComments, "\n");
            return new BlockComment(commentBlock);
        }

    }

}
