package br.com.uberhub.mentorship.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GenerateVouchersRequest(
        @NotNull(message = "é obrigatório")
        @Min(value = 1, message = "deve ser no mínimo 1")
        @Max(value = 100, message = "deve ser no máximo 100")
        Integer quantity) {
}
