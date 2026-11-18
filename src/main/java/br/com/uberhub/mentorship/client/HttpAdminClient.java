package br.com.uberhub.mentorship.client;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import br.com.uberhub.mentorship.client.dto.AnalyticsEventRequest;
import br.com.uberhub.mentorship.client.dto.NotificationRequest;

/**
 * Implementação REAL do {@link AdminClient}: chama o admin-service via Feign.
 *
 * Só é criada quando services.admin.stub-enabled=false (variável ADMIN_STUB=false).
 *
 * Toda chamada fica dentro de try/catch: se o admin-service cair, der timeout ou
 * responder 4xx/5xx, apenas registramos um WARN e seguimos. O feedback do usuário já
 * foi salvo no nosso banco e não deve ser desfeito por causa de uma notificação.
 */
@Component
@ConditionalOnProperty(name = "services.admin.stub-enabled", havingValue = "false")
public class HttpAdminClient implements AdminClient {

    private static final Logger log = LoggerFactory.getLogger(HttpAdminClient.class);

    private final AdminServiceApi api;

    public HttpAdminClient(AdminServiceApi api) {
        this.api = api;
    }

    @Override
    public void recordEvent(AnalyticsEventType type, String userId, Map<String, Object> metadata) {
        try {
            api.recordEvent(new AnalyticsEventRequest(type.name(), userId, Instant.now(), metadata));
        } catch (RuntimeException ex) {
            // FeignException (4xx/5xx), RetryableException (timeout / conexão recusada) etc.
            log.warn("Falha ao registrar evento {} no admin-service: {}", type, ex.getMessage());
        }
    }

    @Override
    public void sendNotification(String userId, String title, String message, Map<String, String> data) {
        try {
            api.sendNotification(new NotificationRequest(userId, title, message, data));
        } catch (RuntimeException ex) {
            log.warn("Falha ao enviar notificação ao usuário {} via admin-service: {}", userId, ex.getMessage());
        }
    }
}
