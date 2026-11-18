package br.com.uberhub.mentorship.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo de POST /mentorships/{id}/request-change (mentorado pede troca de mentor).
 *
 * A documentação fala em "formulário de justificativa" que vai para o admin analisar,
 * por isso o motivo é obrigatório.
 *
 * Exemplo: { "reason": "Os horários do mentor não batem com os meus" }
 *
 * @param reason justificativa, obrigatória, até 1000 caracteres (coluna change_reason)
 */
public record ChangeRequestRequest(
        @NotBlank(message = "é obrigatório")
        @Size(max = 1000, message = "deve ter no máximo 1000 caracteres")
        String reason) {
}
