package example;

import io.github.outakeshi.indexsmith.client.ClientConfiguration;

public final class GradleConsumer {
    private GradleConsumer() { }

    public static ClientConfiguration configuration(String endpoint) {
        return ClientConfiguration.builder().endpoint(endpoint).build();
    }
}
