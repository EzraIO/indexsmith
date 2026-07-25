package io.github.outakeshi.indexsmith.generator.builder;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.List;
import java.util.ArrayList;

/**
 */
public class ClassOrInterfaceTypeBuilder {


    private static final String TEMPLATE = "{0}<{1}>";

    public static Builder builder(String name) {
        return new Builder(name);
    }

    @NoArgsConstructor
    public static class Builder {
        private String name;
        private List<String> typeArguments = new ArrayList<>();


        public Builder(String name) {
            this.name = name;
        }

        public Builder addTypeArgument(String typeArgument) {
            this.typeArguments.add(typeArgument);
            return this;
        }

        public String build() {
            return MessageFormat.format(TEMPLATE, name, StringUtils.join(typeArguments, ", "));
        }
    }
}
