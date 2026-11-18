package br.com.uberhub.mentorship.dto.request;

import br.com.uberhub.mentorship.model.MentorshipStatus;
import jakarta.validation.constraints.NotNull;

/**
 * @param status novo status desejado para a mentoria.
 *               Este endpoint só aceita transições manuais para {@code CANCELLED} e
 *               {@code COMPLETED}; os status {@code SCHEDULED} e {@code CONFIRMED} são
 *               definidos pelos endpoints dedicados de agendamento.
 */
public record UpdateMentorshipStatusRequest(
        @NotNull(message = "é obrigatório")
        MentorshipStatus status) {
}
