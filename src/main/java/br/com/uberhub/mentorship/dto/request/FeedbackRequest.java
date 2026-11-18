package br.com.uberhub.mentorship.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo de POST /mentorships/{id}/feedback/mentor e POST /mentorships/{id}/feedback/mentee.
 *
 * O mesmo formato serve para os dois lados porque no banco os dois feedbacks usam o
 * mesmo embeddable {@link br.com.uberhub.mentorship.model.Feedback} (rating, comment, createdAt).
 *
 * Exemplo: { "rating": 5, "comment": "Sessão muito produtiva" }
 *
 * As validações abaixo são checadas automaticamente pelo @Valid no controller; se
 * falharem, o GlobalExceptionHandler devolve 400 com a lista de erros em "details".
 *
 * @param rating  nota de 1 a 5 (obrigatória)
 * @param comment comentário livre, opcional, até 1000 caracteres (tamanho da coluna no banco)
 */
public record FeedbackRequest(
        @NotNull(message = "é obrigatório")
        @Min(value = 1, message = "deve ser no mínimo 1")
        @Max(value = 5, message = "deve ser no máximo 5")
        Integer rating,

        @Size(max = 1000, message = "deve ter no máximo 1000 caracteres")
        String comment) {
}
