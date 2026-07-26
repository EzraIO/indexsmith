package io.github.indexsmith.client;

import java.io.IOException;
import java.util.List;

final class ConfiguredIndexSmithClient implements IndexSmithClient {
    private final IndexSmithClient delegate;
    private final List<QueryPolicy> policies;
    private final List<RequestListener> listeners;

    ConfiguredIndexSmithClient(IndexSmithClient delegate, ClientConfiguration configuration) {
        this.delegate = delegate;
        this.policies = configuration.getQueryPolicies();
        this.listeners = configuration.getRequestListeners();
    }

    @Override
    public String getAdapterId() { return delegate.getAdapterId(); }

    @Override
    public EsVersion getServerVersion() { return delegate.getServerVersion(); }

    @Override
    public ClientResponse perform(ClientRequest request) throws IOException {
        ClientOperation operation = ClientOperation.request(request);
        long started = System.nanoTime();
        notifyListeners(event(RequestEvent.Phase.START, operation, -1, 0, 0, null));
        try {
            validate(operation);
            ClientResponse response = delegate.perform(request);
            RequestEvent.Phase phase = response.isSuccessful()
                    ? RequestEvent.Phase.SUCCESS : RequestEvent.Phase.FAILURE;
            notifyListeners(event(phase, operation, response.getStatusCode(),
                    elapsedMillis(started), response.getRetryCount(), null));
            return response;
        } catch (IOException | RuntimeException exception) {
            notifyListeners(event(RequestEvent.Phase.FAILURE, operation, statusCode(exception), elapsedMillis(started),
                    retryCount(exception), exception.getClass().getName()));
            throw exception;
        }
    }

    @Override
    public SearchResult search(SearchRequest request) throws IOException {
        ClientOperation operation = ClientOperation.search(request);
        long started = System.nanoTime();
        notifyListeners(event(RequestEvent.Phase.START, operation, -1, 0, 0, null));
        try {
            validate(operation);
            SearchResult result = delegate.search(request);
            notifyListeners(event(RequestEvent.Phase.SUCCESS, operation, 200, elapsedMillis(started),
                    result.getRetryCount(), null));
            return result;
        } catch (IOException | RuntimeException exception) {
            notifyListeners(event(RequestEvent.Phase.FAILURE, operation, statusCode(exception), elapsedMillis(started),
                    retryCount(exception), exception.getClass().getName()));
            throw exception;
        }
    }

    @Override
    public void close() throws IOException { delegate.close(); }

    private void validate(ClientOperation operation) throws QueryPolicyViolationException {
        for (QueryPolicy policy : policies) policy.validate(operation);
    }

    private RequestEvent event(RequestEvent.Phase phase, ClientOperation operation, int statusCode,
                               long durationMillis, int retryCount, String failureType) {
        return new RequestEvent(phase, delegate.getAdapterId(), operation, statusCode,
                durationMillis, retryCount, failureType);
    }

    private void notifyListeners(RequestEvent event) {
        for (RequestListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException ignored) {
                // Observability must never alter request behavior.
            }
        }
    }

    private static long elapsedMillis(long started) {
        return Math.max(0, (System.nanoTime() - started) / 1_000_000L);
    }

    private static int retryCount(Throwable failure) {
        ClientRequestException requestFailure = requestFailure(failure);
        if (requestFailure != null) return requestFailure.getRetryCount();
        int attempts = countFailures(failure);
        return Math.max(0, attempts - 1);
    }

    private static int statusCode(Throwable failure) {
        ClientRequestException requestFailure = requestFailure(failure);
        return requestFailure == null ? -1 : requestFailure.getStatusCode();
    }

    private static ClientRequestException requestFailure(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof ClientRequestException) return (ClientRequestException) current;
            current = current.getCause();
        }
        return null;
    }

    private static int countFailures(Throwable failure) {
        if (failure == null) return 0;
        int count = 1;
        for (Throwable suppressed : failure.getSuppressed()) count += countFailures(suppressed);
        return count;
    }
}
