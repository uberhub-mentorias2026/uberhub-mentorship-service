package br.com.uberhub.mentorship.service;

import org.springframework.stereotype.Service;

import br.com.uberhub.mentorship.repository.VoucherRepository;

@Service
public class VoucherService {

    private final VoucherRepository repository;

    public VoucherService(VoucherRepository repository) {
        this.repository = repository;
    }

    // TODO: generate(quantity), redeem(code, menteeId), validate(code), grantToMentor(mentorId)
}
