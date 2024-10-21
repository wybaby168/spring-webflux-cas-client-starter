package dev.flyfish.boot.cas.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * webflux的session mapping存储
 *
 * @author wangyu
 */
public interface SessionMappingStorage {

    Mono<WebSession> removeSessionByMappingId(String mappingId);

    Mono<Void> removeBySessionById(String mappingId);

    Mono<Void> addSessionById(String mappingId, WebSession session);

    @Slf4j
    class HashMapBackedSessionStorage implements SessionMappingStorage {

        private final Map<String, WebSession> MANAGED_SESSIONS = new HashMap<>();
        private final Map<String, String> ID_TO_SESSION_KEY_MAPPING = new HashMap<>();

        @Override
        public Mono<WebSession> removeSessionByMappingId(String mappingId) {
            WebSession session = this.MANAGED_SESSIONS.get(mappingId);
            if (session != null) {
                return this.removeBySessionById(session.getId()).thenReturn(session);
            }
            return Mono.empty();
        }

        @Override
        public Mono<Void> removeBySessionById(String sessionId) {
            if (log.isDebugEnabled()) {
                log.debug("Attempting to remove Session=[" + sessionId + "]");
            }

            String key = this.ID_TO_SESSION_KEY_MAPPING.get(sessionId);
            if (log.isDebugEnabled()) {
                if (key != null) {
                    log.debug("Found mapping for session.  Session Removed.");
                } else {
                    log.debug("No mapping for session found.  Ignoring.");
                }
            }

            this.MANAGED_SESSIONS.remove(key);
            this.ID_TO_SESSION_KEY_MAPPING.remove(sessionId);
            return Mono.empty();
        }

        @Override
        public Mono<Void> addSessionById(String mappingId, WebSession session) {
            this.ID_TO_SESSION_KEY_MAPPING.put(session.getId(), mappingId);
            this.MANAGED_SESSIONS.put(mappingId, session);
            return Mono.empty();
        }
    }
}
