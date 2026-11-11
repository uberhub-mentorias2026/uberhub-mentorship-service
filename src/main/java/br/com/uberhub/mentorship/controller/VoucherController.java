package br.com.uberhub.mentorship.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.uberhub.mentorship.service.VoucherService;

@RestController
@RequestMapping("/api/v1/vouchers")
public class VoucherController {

    private final VoucherService service;

    public VoucherController(VoucherService service) {
        this.service = service;
    }

    // TODO: POST /redeem, GET /validate/{code}, endpoint interno de geração (chamado pelo admin-service)
}
