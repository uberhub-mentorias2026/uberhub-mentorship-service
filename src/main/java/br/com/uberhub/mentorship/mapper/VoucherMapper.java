package br.com.uberhub.mentorship.mapper;

import org.springframework.stereotype.Component;

import br.com.uberhub.mentorship.dto.response.VoucherResponse;
import br.com.uberhub.mentorship.model.Voucher;

@Component
public class VoucherMapper {

    public VoucherResponse toResponse(Voucher voucher) {
        return new VoucherResponse(
                voucher.getId(),
                voucher.getCode(),
                voucher.getStatus(),
                voucher.getCreatedAt(),
                voucher.getRedeemedAt());
    }
}
