package br.com.uberhub.mentorship.dto.response;

import br.com.uberhub.mentorship.model.VoucherStatus;

public record ValidateVoucherResponse(String code, boolean valid, VoucherStatus status) {
}
