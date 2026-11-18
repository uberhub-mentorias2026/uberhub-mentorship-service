package br.com.uberhub.mentorship.dto.response;

import java.time.Instant;

import br.com.uberhub.mentorship.model.MentorshipStatus;

/**
 * Resposta de POST /mentorships/{id}/request-change e itens de GET /mentorships/change-requests.
 *
 * Traz os ids do mentor e do mentorado porque o admin-service (Equipe C) usa esta
 * lista na tela "pedidos de troca" (GET /admin/mentorships/change-requests).
 *
 * @param mentorshipId id da mentoria
 * @param menteeId     quem pediu a troca
 * @param mentorId     mentor atual (o que será trocado)
 * @param topic        assunto da mentoria
 * @param status       status atual (CHANGE_REQUESTED enquanto o admin não decide)
 * @param reason       justificativa do mentorado
 * @param requestedAt  quando o pedido foi feito
 */
public record ChangeRequestResponse(
        Long mentorshipId,
        String menteeId,
        String mentorId,
        String topic,
        MentorshipStatus status,
        String reason,
        Instant requestedAt) {
}
