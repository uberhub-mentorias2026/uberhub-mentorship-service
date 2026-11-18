package br.com.uberhub.mentorship.client.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Corpo enviado ao admin-service em POST /api/v1/analytics/record-event.
 *
 * Os campos espelham a coleção "analytics" descrita na documentação do admin-service
 * (eventType, userId, timestamp, metadata). O JSON exato do endpoint ainda NÃO foi
 * publicado pela Equipe C, então este formato é a nossa proposta.
 *
 * Exemplo:
 * <pre>
 * {
 *   "eventType": "MENTORSHIP_COMPLETED",
 *   "userId": "id-do-mentor",
 *   "timestamp": "2026-12-01T20:00:00Z",
 *   "metadata": { "mentorshipId": 10, "menteeId": "id-do-mentorado", "rating": 5 }
 * }
 * </pre>
 *
 * @param eventType tipo do evento (nome de {@link br.com.uberhub.mentorship.client.AnalyticsEventType})
 * @param userId    usuário que gerou o evento (quem fez a ação)
 * @param timestamp momento em que o evento aconteceu
 * @param metadata  dados extras livres (ids, nota etc.), usados pelo dashboard
 */
public record AnalyticsEventRequest(
        String eventType,
        String userId,
        Instant timestamp,
        Map<String, Object> metadata) {
}
