package io.github.outakeshi.indexsmith.generator;

import org.junit.Test;

public class GeneratedRepositoryEs8IT extends GeneratedRepositoryIntegrationSupport {
    @Test
    public void runsGeneratedRepositoryAgainstEs8() throws Exception {
        verifiesGeneratedRepository(System.getProperty("indexsmith.test.es8.image",
                "docker.elastic.co/elasticsearch/elasticsearch:8.17.3"), "es8");
    }
}
