package br.com.uberhub.mentorship.dto.response;

import java.time.Instant;

import br.com.uberhub.mentorship.model.MentorshipStatus;

/**
 * Visão retornada pelos endpoints de agendamento e status (PUT /schedule,
 * POST /schedule/confirm, PUT /status).
 *
 * Propositalmente não inclui topic/tags/feedback (visão completa da mentoria):
 * isso fica a cargo do MentorshipResponse que o Manzi vai criar junto com
 * POST /mentorships e GET /mentorships/mentee|mentor, pra não colidir com o
 * arquivo dele. Se/quando o time unificar as respostas, dá pra substituir
 * este record pelo MentorshipResponse comum.
 */
public record MentorshipScheduleResponse(
        Long id,
        String mentorId,
        String menteeId,
        MentorshipStatus status,
        Instant scheduleDateTime,
        Boolean scheduleConfirmedByMentee,
        Instant updatedAt) {
}
