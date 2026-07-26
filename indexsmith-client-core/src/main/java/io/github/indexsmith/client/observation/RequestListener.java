package io.github.indexsmith.client.observation;

/** Receives best-effort, redacted request lifecycle events. Listener failures never fail a request. */
public interface RequestListener {
    void onEvent(RequestEvent event);
}
