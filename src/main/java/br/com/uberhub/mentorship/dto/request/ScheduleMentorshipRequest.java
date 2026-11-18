package br.com.uberhub.mentorship.dto.request;

import java.time.Instant;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

/**
 * @param dateTime data/hora proposta pelo mentor para a sessão. Deve ser no futuro.
 */
public record ScheduleMentorshipRequest(
        @NotNull(message = "é obrigatório")
        @Future(message = "deve ser uma data/hora futura")
        Instant dateTime) {
}
