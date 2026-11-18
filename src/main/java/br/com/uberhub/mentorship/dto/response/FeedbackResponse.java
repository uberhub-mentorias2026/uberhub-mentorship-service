package br.com.uberhub.mentorship.dto.response;

import java.time.Instant;

import br.com.uberhub.mentorship.model.MentorshipStatus;

/**
 * Resposta dos endpoints de feedback (mentor e mentee).
 *
 * Usamos um DTO próprio (em vez do DTO geral da mentoria, que é da frente do Manzi)
 * para não criar dependência/conflito entre as frentes.
 *
 * Exemplo:
 * <pre>
 * { "mentorshipId": 10, "status": "COMPLETED", "rating": 5,
 *   "comment": "Ótima sessão", "createdAt": "2026-12-01T20:00:00Z" }
 * </pre>
 *
 * @param mentorshipId id da mentoria avaliada
 * @param status       status da mentoria DEPOIS do feedback
 * @param rating       nota registrada
 * @param comment      comentário registrado (pode ser null)
 * @param createdAt    quando o feedback foi registrado
 */
public record FeedbackResponse(
        Long mentorshipId,
        MentorshipStatus status,
        Integer rating,
        String comment,
        Instant createdAt) {
}
