package br.com.uberhub.mentorship.dto.response;

import java.time.Instant;

import br.com.uberhub.mentorship.model.VoucherStatus;

/** Visão pública do voucher. Não expõe quem resgatou (redeemedBy). */
public record VoucherResponse(
        Long id,
        String code,
        VoucherStatus status,
        Instant createdAt,
        Instant redeemedAt) {
}
