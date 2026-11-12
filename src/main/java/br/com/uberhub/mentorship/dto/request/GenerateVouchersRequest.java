package br.com.uberhub.mentorship.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param quantity quantidade de vouchers a gerar (1 a 100)
 * @param ownerId  id do mentor que receberá os vouchers; opcional (sem dono = disponível no pool do admin)
 */
public record GenerateVouchersRequest(
        @NotNull(message = "é obrigatório")
        @Min(value = 1, message = "deve ser no mínimo 1")
        @Max(value = 100, message = "deve ser no máximo 100")
        Integer quantity,

        @Size(max = 64, message = "deve ter no máximo 64 caracteres")
        String ownerId) {
}
