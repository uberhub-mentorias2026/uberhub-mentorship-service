package br.com.uberhub.mentorship.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.uberhub.mentorship.dto.response.ValidateVoucherResponse;
import br.com.uberhub.mentorship.dto.response.VoucherResponse;
import br.com.uberhub.mentorship.exception.ConflictException;
import br.com.uberhub.mentorship.exception.NotFoundException;
import br.com.uberhub.mentorship.mapper.VoucherMapper;
import br.com.uberhub.mentorship.model.Voucher;
import br.com.uberhub.mentorship.model.VoucherStatus;
import br.com.uberhub.mentorship.repository.VoucherRepository;

@Service
public class VoucherService {

    // Sem caracteres ambíguos (0/O, 1/I) para evitar erro de digitação do usuário
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;   // formato XXXX-XXXX
    private static final int GROUP_SIZE = 4;

    private final SecureRandom random = new SecureRandom();

    private final VoucherRepository repository;
    private final VoucherMapper mapper;

    public VoucherService(VoucherRepository repository, VoucherMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /** Gera vouchers novos (AVAILABLE). Chamado pelo admin-service. */
    @Transactional
    public List<VoucherResponse> generate(int quantity) {
        Set<String> codes = new LinkedHashSet<>();
        while (codes.size() < quantity) {
            String code = newCode();
            if (!repository.existsByCode(code)) {
                codes.add(code);
            }
        }

        List<Voucher> vouchers = codes.stream()
                .map(code -> Voucher.builder().code(code).status(VoucherStatus.AVAILABLE).build())
                .toList();

        return repository.saveAll(vouchers).stream().map(mapper::toResponse).toList();
    }

    /** Mentorado resgata um voucher. O menteeId vem do token JWT, nunca do body. */
    @Transactional
    public VoucherResponse redeem(String rawCode, String menteeId) {
        String code = normalize(rawCode);

        int updated = repository.markAsRedeemed(code, menteeId, Instant.now());
        if (updated == 0) {
            if (repository.existsByCode(code)) {
                throw new ConflictException("Voucher já foi utilizado");
            }
            throw new NotFoundException("Voucher não encontrado");
        }

        Voucher voucher = repository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Voucher não encontrado"));
        return mapper.toResponse(voucher);
    }

    @Transactional(readOnly = true)
    public ValidateVoucherResponse validate(String rawCode) {
        Voucher voucher = repository.findByCode(normalize(rawCode))
                .orElseThrow(() -> new NotFoundException("Voucher não encontrado"));
        return new ValidateVoucherResponse(
                voucher.getCode(),
                voucher.getStatus() == VoucherStatus.AVAILABLE,
                voucher.getStatus());
    }

    // TODO (Marco 3): grantToMentor(mentorId) - chamado ao registrar o feedback do mentor

    private String newCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            if (i == GROUP_SIZE) {
                sb.append('-');
            }
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private String normalize(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
