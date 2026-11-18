package br.com.uberhub.mentorship.client;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Implementação FALSA do {@link AdminClient} para desenvolvimento e testes.
 *
 * Ativa por padrão (matchIfMissing = true) enquanto o admin-service não publica os
 * endpoints internos. Não faz chamada de rede: só escreve no log o que SERIA enviado,
 * o que ajuda a conferir o fluxo rodando localmente.
 */
@Component
@ConditionalOnProperty(name = "services.admin.stub-enabled", havingValue = "true", matchIfMissing = true)
public class StubAdminClient implements AdminClient {

    private static final Logger log = LoggerFactory.getLogger(StubAdminClient.class);

    @Override
    public void recordEvent(AnalyticsEventType type, String userId, Map<String, Object> metadata) {
        log.info("[STUB admin] analytics: type={}, userId={}, metadata={}", type, userId, metadata);
    }

    @Override
    public void sendNotification(String userId, String title, String message, Map<String, String> data) {
        log.info("[STUB admin] notificacao: userId={}, title={}, message={}, data={}",
                userId, title, message, data);
    }
}
