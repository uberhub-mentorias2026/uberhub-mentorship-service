package br.com.uberhub.mentorship.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.uberhub.mentorship.dto.response.ValidateVoucherResponse;
import br.com.uberhub.mentorship.dto.response.VoucherResponse;
import br.com.uberhub.mentorship.exception.ConflictException;
import br.com.uberhub.mentorship.exception.NotFoundException;
import br.com.uberhub.mentorship.mapper.VoucherMapper;
import br.com.uberhub.mentorship.model.Voucher;
import br.com.uberhub.mentorship.model.VoucherStatus;
import br.com.uberhub.mentorship.repository.VoucherRepository;

@ExtendWith(MockitoExtension.class)
class VoucherServiceTest {

    @Mock
    VoucherRepository repository;

    VoucherService service;

    @BeforeEach
    void setUp() {
        service = new VoucherService(repository, new VoucherMapper());
    }

    @Test
    void generate_criaQuantidadePedidaDeCodigosUnicosNoFormatoEsperado() {
        when(repository.existsByCode(anyString())).thenReturn(false);
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<VoucherResponse> result = service.generate(5);

        assertThat(result).hasSize(5);
        assertThat(result.stream().map(VoucherResponse::code).distinct().count()).isEqualTo(5);
        assertThat(result).allMatch(v -> v.code().matches("[A-Z2-9]{4}-[A-Z2-9]{4}"));
        assertThat(result).allMatch(v -> v.status() == VoucherStatus.AVAILABLE);
    }

    @Test
    void redeem_sucesso_normalizaOCodigoERetornaResgatado() {
        Voucher saved = Voucher.builder().id(1L).code("ABCD-2345")
                .status(VoucherStatus.REDEEMED).redeemedBy("mentee-1").redeemedAt(Instant.now()).build();
        when(repository.markAsRedeemed(eq("ABCD-2345"), eq("mentee-1"), any(Instant.class))).thenReturn(1);
        when(repository.findByCode("ABCD-2345")).thenReturn(Optional.of(saved));

        VoucherResponse response = service.redeem("  abcd-2345 ", "mentee-1");

        assertThat(response.status()).isEqualTo(VoucherStatus.REDEEMED);
        assertThat(response.code()).isEqualTo("ABCD-2345");
    }

    @Test
    void redeem_jaUtilizado_lancaConflict() {
        when(repository.markAsRedeemed(eq("ABCD-2345"), eq("mentee-1"), any(Instant.class))).thenReturn(0);
        when(repository.existsByCode("ABCD-2345")).thenReturn(true);

        assertThatThrownBy(() -> service.redeem("ABCD-2345", "mentee-1"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void redeem_inexistente_lancaNotFound() {
        when(repository.markAsRedeemed(eq("XXXX-0000"), eq("mentee-1"), any(Instant.class))).thenReturn(0);
        when(repository.existsByCode("XXXX-0000")).thenReturn(false);

        assertThatThrownBy(() -> service.redeem("XXXX-0000", "mentee-1"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void validate_disponivelERetornaValidoTrue() {
        Voucher v = Voucher.builder().code("ABCD-2345").status(VoucherStatus.AVAILABLE).build();
        when(repository.findByCode("ABCD-2345")).thenReturn(Optional.of(v));

        ValidateVoucherResponse response = service.validate("abcd-2345");

        assertThat(response.valid()).isTrue();
    }
}
