package br.com.uberhub.mentorship.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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

    private static final String CODE = "ABCD-2345-WXYZ";

    @Mock
    VoucherRepository repository;

    @Captor
    ArgumentCaptor<List<Voucher>> savedCaptor;

    VoucherService service;

    @BeforeEach
    void setUp() {
        service = new VoucherService(repository, new VoucherMapper());
    }

    @Test
    void generate_criaCodigosUnicosNoFormatoXXXX_XXXX_XXXXComDono() {
        when(repository.existsByCode(anyString())).thenReturn(false);
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<VoucherResponse> result = service.generate(5, "mentor-1");

        assertThat(result).hasSize(5);
        assertThat(result.stream().map(VoucherResponse::code).distinct().count()).isEqualTo(5);
        assertThat(result).allMatch(v -> v.code().matches("[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}"));
        assertThat(result).allMatch(v -> v.code().length() == 14);
        assertThat(result).allMatch(v -> v.status() == VoucherStatus.AVAILABLE);

        verify(repository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).allMatch(v -> "mentor-1".equals(v.getOwnerId()));
    }

    @Test
    void generate_semDono_salvaComOwnerNulo() {
        when(repository.existsByCode(anyString())).thenReturn(false);
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        service.generate(2, "   ");

        verify(repository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).allMatch(v -> v.getOwnerId() == null);
    }

    @Test
    void listByOwner_retornaVouchersDoMentor() {
        Voucher v1 = Voucher.builder().id(1L).code(CODE).status(VoucherStatus.AVAILABLE).ownerId("mentor-1").build();
        Voucher v2 = Voucher.builder().id(2L).code("ZZZZ-2345-WXYZ").status(VoucherStatus.REDEEMED).ownerId("mentor-1").build();
        when(repository.findByOwnerIdOrderByCreatedAtDesc("mentor-1")).thenReturn(List.of(v1, v2));

        List<VoucherResponse> result = service.listByOwner("mentor-1");

        assertThat(result).hasSize(2);
    }

    @Test
    void redeem_sucesso_normalizaOCodigoERetornaResgatado() {
        Voucher saved = Voucher.builder().id(1L).code(CODE)
                .status(VoucherStatus.REDEEMED).redeemedBy("mentee-1").redeemedAt(Instant.now()).build();
        when(repository.markAsRedeemed(eq(CODE), eq("mentee-1"), any(Instant.class))).thenReturn(1);
        when(repository.findByCode(CODE)).thenReturn(Optional.of(saved));

        VoucherResponse response = service.redeem("  abcd-2345-wxyz ", "mentee-1");

        assertThat(response.status()).isEqualTo(VoucherStatus.REDEEMED);
        assertThat(response.code()).isEqualTo(CODE);
    }

    @Test
    void redeem_jaUtilizado_lancaConflict() {
        when(repository.markAsRedeemed(eq(CODE), eq("mentee-1"), any(Instant.class))).thenReturn(0);
        when(repository.existsByCode(CODE)).thenReturn(true);

        assertThatThrownBy(() -> service.redeem(CODE, "mentee-1"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void redeem_inexistente_lancaNotFound() {
        when(repository.markAsRedeemed(eq("XXXX-0000-YYYY"), eq("mentee-1"), any(Instant.class))).thenReturn(0);
        when(repository.existsByCode("XXXX-0000-YYYY")).thenReturn(false);

        assertThatThrownBy(() -> service.redeem("XXXX-0000-YYYY", "mentee-1"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void validate_disponivelRetornaValidoTrue() {
        Voucher v = Voucher.builder().code(CODE).status(VoucherStatus.AVAILABLE).build();
        when(repository.findByCode(CODE)).thenReturn(Optional.of(v));

        ValidateVoucherResponse response = service.validate("abcd-2345-wxyz");

        assertThat(response.valid()).isTrue();
    }
}
