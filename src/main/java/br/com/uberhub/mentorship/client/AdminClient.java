package br.com.uberhub.mentorship.client;

import java.util.Map;

/**
 * Porta de comunicação com o admin-service (Equipe C).
 *
 * O admin-service tem dois endpoints INTERNOS (não passam pelo API Gateway) que o
 * mentorship-service usa:
 * - POST /api/v1/analytics/record-event -> grava eventos de negócio para o dashboard;
 * - POST /api/v1/notifications/send     -> envia push (FCM) para um usuário.
 *
 * Regra importante: notificação e analytics são EFEITOS COLATERAIS. Se o admin-service
 * estiver fora do ar, o feedback/pedido de troca do usuário NÃO pode falhar por isso.
 * Por isso os métodos não lançam exceção: as implementações registram o erro no log e seguem.
 *
 * Implementações:
 * - {@link StubAdminClient}: padrão em desenvolvimento, só escreve no log;
 * - {@link HttpAdminClient}: chama o admin-service de verdade (services.admin.stub-enabled=false).
 */
public interface AdminClient {

    /**
     * Registra um evento de negócio no analytics do admin-service.
     *
     * @param type     tipo do evento
     * @param userId   quem executou a ação
     * @param metadata dados extras (ids, nota...). Não pode conter valores null.
     */
    void recordEvent(AnalyticsEventType type, String userId, Map<String, Object> metadata);

    /**
     * Pede ao admin-service que envie uma notificação push ao usuário.
     *
     * @param userId  destinatário
     * @param title   título do push
     * @param message corpo do push
     * @param data    dados extras para o app (ex.: mentorshipId para abrir a tela certa)
     */
    void sendNotification(String userId, String title, String message, Map<String, String> data);
}
