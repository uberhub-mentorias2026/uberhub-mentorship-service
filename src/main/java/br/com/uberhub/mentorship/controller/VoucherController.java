package br.com.uberhub.mentorship.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.uberhub.mentorship.dto.request.GenerateVouchersRequest;
import br.com.uberhub.mentorship.dto.request.RedeemVoucherRequest;
import br.com.uberhub.mentorship.dto.response.ValidateVoucherResponse;
import br.com.uberhub.mentorship.dto.response.VoucherResponse;
import br.com.uberhub.mentorship.service.VoucherService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/vouchers")
public class VoucherController {

    private final VoucherService service;

    public VoucherController(VoucherService service) {
        this.service = service;
    }

    /** Interno: chamado pelo admin-service (POST /admin/vouchers/generate). */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<VoucherResponse>> generate(@Valid @RequestBody GenerateVouchersRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.generate(request.quantity()));
    }

    /** Mentorado resgata um voucher. */
    @PostMapping("/redeem")
    @PreAuthorize("hasRole('MENTEE')")
    public VoucherResponse redeem(@Valid @RequestBody RedeemVoucherRequest request,
                                  @AuthenticationPrincipal Jwt jwt) {
        return service.redeem(request.code(), jwt.getSubject());
    }

    /** Qualquer usuário autenticado pode consultar se um código está disponível. */
    @GetMapping("/validate/{code}")
    @PreAuthorize("isAuthenticated()")
    public ValidateVoucherResponse validate(@PathVariable String code) {
        return service.validate(code);
    }
}
