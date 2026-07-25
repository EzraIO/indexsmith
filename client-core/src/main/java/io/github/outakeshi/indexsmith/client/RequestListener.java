package io.github.outakeshi.indexsmith.client;

/** Receives best-effort, redacted request lifecycle events. Listener failures never fail a request. */
public interface RequestListener {
    void onEvent(RequestEvent event);
}
