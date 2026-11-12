package br.com.uberhub.mentorship.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RedeemVoucherRequest(
        @NotBlank(message = "é obrigatório")
        @Pattern(regexp = "(?i)[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}",
                 message = "formato inválido, use XXXX-XXXX-XXXX")
        String code) {
}
