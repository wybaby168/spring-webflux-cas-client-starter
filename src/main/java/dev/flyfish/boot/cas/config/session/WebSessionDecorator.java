package dev.flyfish.boot.cas.config.session;

import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class WebSessionDecorator implements WebSession {

    private final WebSession decorated;

    private final List<WebSessionListener> listeners;

    /**
     * Return a unique session identifier.
     */
    @Override
    public String getId() {
        return decorated.getId();
    }

    /**
     * Return a map that holds session attributes.
     */
    @Override
    public Map<String, Object> getAttributes() {
        return decorated.getAttributes();
    }

    /**
     * Return the session attribute value if present.
     *
     * @param name the attribute name
     * @return the attribute value
     */
    @Nullable
    @Override
    public <T> T getAttribute(String name) {
        return decorated.getAttribute(name);
    }

    /**
     * Return the session attribute value or if not present raise an
     * {@link IllegalArgumentException}.
     *
     * @param name the attribute name
     * @return the attribute value
     */
    @Override
    public <T> T getRequiredAttribute(String name) {
        return decorated.getRequiredAttribute(name);
    }

    /**
     * Return the session attribute value, or a default, fallback value.
     *
     * @param name         the attribute name
     * @param defaultValue a default value to return instead
     * @return the attribute value
     */
    @Override
    public <T> T getAttributeOrDefault(String name, T defaultValue) {
        return decorated.getAttributeOrDefault(name, defaultValue);
    }

    /**
     * Force the creation of a session causing the session id to be sent when
     * {@link #save()} is called.
     */
    @Override
    public void start() {
        decorated.start();
    }

    /**
     * Whether a session with the client has been started explicitly via
     * {@link #start()} or implicitly by adding session attributes.
     * If "false" then the session id is not sent to the client and the
     * {@link #save()} method is essentially a no-op.
     */
    @Override
    public boolean isStarted() {
        return decorated.isStarted();
    }

    /**
     * Generate a new id for the session and update the underlying session
     * storage to reflect the new id. After a successful call {@link #getId()}
     * reflects the new session id.
     *
     * @return completion notification (success or error)
     */
    @Override
    public Mono<Void> changeSessionId() {
        return decorated.changeSessionId();
    }

    /**
     * Invalidate the current session and clear session storage.
     *
     * @return completion notification (success or error)
     */
    @Override
    public Mono<Void> invalidate() {
        // 后续处理
        Mono<Void> consumer = Mono.defer(() -> listeners.stream()
                .map(listener -> listener.onSessionInvalidated(this.decorated))
                .reduce(Mono::then)
                .orElse(Mono.empty()));

        return decorated.invalidate().then(consumer);
    }

    /**
     * Save the session through the {@code WebSessionStore} as follows:
     * <ul>
     * <li>If the session is new (i.e. created but never persisted), it must have
     * been started explicitly via {@link #start()} or implicitly by adding
     * attributes, or otherwise this method should have no effect.
     * <li>If the session was retrieved through the {@code WebSessionStore},
     * the implementation for this method must check whether the session was
     * {@link #invalidate() invalidated} and if so return an error.
     * </ul>
     * <p>Note that this method is not intended for direct use by applications.
     * Instead it is automatically invoked just before the response is
     * committed.
     *
     * @return {@code Mono} to indicate completion with success or error
     */
    @Override
    public Mono<Void> save() {
        return decorated.save();
    }

    /**
     * Return {@code true} if the session expired after {@link #getMaxIdleTime()
     * maxIdleTime} elapsed.
     * <p>Typically expiration checks should be automatically made when a session
     * is accessed, a new {@code WebSession} instance created if necessary, at
     * the start of request processing so that applications don't have to worry
     * about expired session by default.
     */
    @Override
    public boolean isExpired() {
        return decorated.isExpired();
    }

    /**
     * Return the time when the session was created.
     */
    @Override
    public Instant getCreationTime() {
        return decorated.getCreationTime();
    }

    /**
     * Return the last time of session access as a result of user activity such
     * as an HTTP request. Together with {@link #getMaxIdleTime()
     * maxIdleTimeInSeconds} this helps to determine when a session is
     * {@link #isExpired() expired}.
     */
    @Override
    public Instant getLastAccessTime() {
        return decorated.getLastAccessTime();
    }

    /**
     * Configure the max amount of time that may elapse after the
     * {@link #getLastAccessTime() lastAccessTime} before a session is considered
     * expired. A negative value indicates the session should not expire.
     *
     * @param maxIdleTime
     */
    @Override
    public void setMaxIdleTime(Duration maxIdleTime) {
        decorated.setMaxIdleTime(maxIdleTime);
    }

    /**
     * Return the maximum time after the {@link #getLastAccessTime()
     * lastAccessTime} before a session expires. A negative time indicates the
     * session doesn't expire.
     */
    @Override
    public Duration getMaxIdleTime() {
        return decorated.getMaxIdleTime();
    }
}
