package io.github.outakeshi.indexsmith.generator;

import org.junit.Test;

public class GeneratedRepositoryEs7IT extends GeneratedRepositoryIntegrationSupport {
    @Test
    public void runsGeneratedRepositoryAgainstEs7() throws Exception {
        verifiesGeneratedRepository(System.getProperty("indexsmith.test.es7.image",
                "docker.elastic.co/elasticsearch/elasticsearch:7.17.22"), "es7");
    }
}
