package io.github.indexsmith.client;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IndexSmithClientFactoryTest {
    private final ClientConfiguration configuration = ClientConfiguration.builder()
            .endpoint("localhost:9200/")
            .build();

    @Test
    public void selectsProviderByDetectedServerMajor() throws Exception {
        IndexSmithClientFactory factory = new IndexSmithClientFactory(
                ignored -> EsVersion.parse("8.17.3"),
                Arrays.asList(provider("es7", 7), provider("es8", 8)));

        try (IndexSmithClient client = factory.create(configuration)) {
            assertEquals("es8", client.getAdapterId());
            assertEquals("8.17.3", client.getServerVersion().toString());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsExplicitIncompatibleAdapter() throws Exception {
        ClientConfiguration es7Only = ClientConfiguration.builder()
                .endpoint("localhost:9200")
                .adapter("es7")
                .build();
        IndexSmithClientFactory factory = new IndexSmithClientFactory(
                ignored -> EsVersion.parse("8.17.3"),
                Arrays.asList(provider("es7", 7), provider("es8", 8)));
        factory.create(es7Only);
    }

    @Test
    public void normalizesEndpoint() {
        assertEquals("http://localhost:9200", configuration.getEndpoints().get(0));
    }

    @Test
    public void appliesPoliciesAndPublishesRedactedLifecycleEvents() throws Exception {
        List<RequestEvent> events = new ArrayList<>();
        ClientConfiguration protectedConfiguration = ClientConfiguration.builder()
                .endpoint("localhost:9200")
                .queryPolicy(operation -> {
                    if (operation.getPath().contains("forbidden")) {
                        throw new QueryPolicyViolationException("blocked");
                    }
                })
                .requestListener(events::add)
                .build();
        IndexSmithClientFactory factory = new IndexSmithClientFactory(
                ignored -> EsVersion.parse("8.17.3"), Arrays.asList(provider("es8", 8, 2)));

        try (IndexSmithClient client = factory.create(protectedConfiguration)) {
            client.perform(ClientRequest.json("POST", "/orders/_search?source=secret#fragment",
                    "{\"password\":\"secret\"}"));
            try {
                client.perform(ClientRequest.get("/forbidden"));
                fail("Expected QueryPolicyViolationException");
            } catch (QueryPolicyViolationException expected) {
                // expected
            }
        }

        assertEquals(4, events.size());
        assertEquals(RequestEvent.Phase.START, events.get(0).getPhase());
        assertEquals(RequestEvent.Phase.SUCCESS, events.get(1).getPhase());
        assertEquals(200, events.get(1).getStatusCode());
        assertEquals(2, events.get(1).getRetryCount());
        assertEquals("/orders/_search", events.get(1).getPath());
        assertEquals(RequestEvent.Phase.FAILURE, events.get(3).getPhase());
        assertEquals(QueryPolicyViolationException.class.getName(), events.get(3).getFailureType());
        for (RequestEvent event : events) {
            assertTrue(!event.getPath().contains("secret"));
        }
    }

    @Test
    public void appliesPolicyToSearchEntryPoint() throws Exception {
        ClientConfiguration protectedConfiguration = ClientConfiguration.builder()
                .endpoint("localhost:9200")
                .queryPolicy(operation -> {
                    if (operation.getType() == ClientOperation.Type.SEARCH) {
                        throw new QueryPolicyViolationException("search blocked");
                    }
                }).build();
        IndexSmithClientFactory factory = new IndexSmithClientFactory(
                ignored -> EsVersion.parse("8.17.3"), Arrays.asList(provider("es8", 8)));

        try (IndexSmithClient client = factory.create(protectedConfiguration)) {
            try {
                client.search(new SearchRequest("orders", "{}"));
                fail("Expected QueryPolicyViolationException");
            } catch (QueryPolicyViolationException expected) {
                assertEquals("search blocked", expected.getMessage());
            }
        }
    }

    @Test
    public void publishesHttpFailureStatusWithoutChangingResponseSemantics() throws Exception {
        List<RequestEvent> events = new ArrayList<>();
        ClientConfiguration observedConfiguration = ClientConfiguration.builder()
                .endpoint("localhost:9200")
                .requestListener(events::add)
                .build();
        IndexSmithClientFactory factory = new IndexSmithClientFactory(
                ignored -> EsVersion.parse("8.17.3"), Arrays.asList(provider("es8", 8, 1, 503)));

        try (IndexSmithClient client = factory.create(observedConfiguration)) {
            assertEquals(503, client.perform(ClientRequest.get("/orders")).getStatusCode());
        }

        assertEquals(2, events.size());
        assertEquals(RequestEvent.Phase.FAILURE, events.get(1).getPhase());
        assertEquals(503, events.get(1).getStatusCode());
        assertEquals(1, events.get(1).getRetryCount());
    }

    @Test
    public void preservesSearchFailureStatusAndRetryCountInEvents() throws Exception {
        List<RequestEvent> events = new ArrayList<>();
        IndexSmithClient delegate = new StubClient("es8", EsVersion.parse("8.17.3"), 0, 200) {
            @Override
            public SearchResult search(SearchRequest request) throws IOException {
                throw new ClientRequestException("unavailable", 503, 2, null);
            }
        };
        ClientConfiguration observedConfiguration = ClientConfiguration.builder()
                .endpoint("localhost:9200")
                .requestListener(events::add)
                .build();

        try (IndexSmithClient client =
                     new ConfiguredIndexSmithClient(delegate, observedConfiguration)) {
            try {
                client.search(new SearchRequest("orders", "{}"));
                fail("Expected ClientRequestException");
            } catch (ClientRequestException expected) {
                assertEquals(503, expected.getStatusCode());
            }
        }

        assertEquals(2, events.size());
        assertEquals(RequestEvent.Phase.FAILURE, events.get(1).getPhase());
        assertEquals(503, events.get(1).getStatusCode());
        assertEquals(2, events.get(1).getRetryCount());
    }

    private static ElasticsearchClientProvider provider(String id, int major) {
        return provider(id, major, 0);
    }

    private static ElasticsearchClientProvider provider(String id, int major, int retryCount) {
        return provider(id, major, retryCount, 200);
    }

    private static ElasticsearchClientProvider provider(String id, int major, int retryCount, int statusCode) {
        return new ElasticsearchClientProvider() {
            @Override
            public String getId() { return id; }

            @Override
            public boolean supports(EsVersion serverVersion) { return serverVersion.getMajor() == major; }

            @Override
            public IndexSmithClient create(ClientConfiguration configuration, EsVersion serverVersion) {
                return new StubClient(id, serverVersion, retryCount, statusCode);
            }
        };
    }

    private static class StubClient implements IndexSmithClient {
        private final String id;
        private final EsVersion version;
        private final int retryCount;
        private final int statusCode;

        private StubClient(String id, EsVersion version, int retryCount, int statusCode) {
            this.id = id;
            this.version = version;
            this.retryCount = retryCount;
            this.statusCode = statusCode;
        }

        @Override
        public String getAdapterId() { return id; }

        @Override
        public EsVersion getServerVersion() { return version; }

        @Override
        public ClientResponse perform(ClientRequest request) {
            return new ClientResponse(statusCode, "{}", retryCount);
        }

        @Override
        public SearchResult search(SearchRequest request) throws IOException { return new SearchResult("{}"); }

        @Override
        public void close() throws IOException { }
    }
}
