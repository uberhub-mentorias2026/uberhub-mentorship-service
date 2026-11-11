package br.com.uberhub.mentorship.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import br.com.uberhub.mentorship.model.Voucher;
import br.com.uberhub.mentorship.model.VoucherStatus;
import br.com.uberhub.mentorship.repository.VoucherRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VoucherControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    VoucherRepository repository;

    @BeforeEach
    void limparBanco() {
        repository.deleteAll();
    }

    private static RequestPostProcessor como(String role, String userId) {
        return jwt().jwt(j -> j.subject(userId)).authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private void criarVoucher(String code) {
        repository.save(Voucher.builder().code(code).status(VoucherStatus.AVAILABLE).build());
    }

    @Test
    void generate_comoAdmin_retorna201ComVouchersDisponiveis() throws Exception {
        mvc.perform(post("/api/v1/vouchers/generate")
                        .with(como("ADMIN", "admin-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));

        assertThat(repository.count()).isEqualTo(3);
    }

    @Test
    void generate_comoMentee_retorna403() throws Exception {
        mvc.perform(post("/api/v1/vouchers/generate")
                        .with(como("MENTEE", "mentee-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void generate_quantidadeInvalida_retorna400() throws Exception {
        mvc.perform(post("/api/v1/vouchers/generate")
                        .with(como("ADMIN", "admin-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void redeem_fluxoCompleto_resgataDepoisRetorna409() throws Exception {
        criarVoucher("TEST-0001");

        mvc.perform(post("/api/v1/vouchers/redeem")
                        .with(como("MENTEE", "mentee-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"test-0001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REDEEMED"));

        Voucher salvo = repository.findByCode("TEST-0001").orElseThrow();
        assertThat(salvo.getRedeemedBy()).isEqualTo("mentee-1");

        mvc.perform(post("/api/v1/vouchers/redeem")
                        .with(como("MENTEE", "mentee-2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"TEST-0001\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void redeem_codigoInexistente_retorna404() throws Exception {
        mvc.perform(post("/api/v1/vouchers/redeem")
                        .with(como("MENTEE", "mentee-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"NAO-EXISTE\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void redeem_comoMentor_retorna403() throws Exception {
        criarVoucher("TEST-0002");

        mvc.perform(post("/api/v1/vouchers/redeem")
                        .with(como("MENTOR", "mentor-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"TEST-0002\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void redeem_semToken_retorna401() throws Exception {
        mvc.perform(post("/api/v1/vouchers/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"TEST-0001\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validate_disponivelDepoisResgatadoFicaInvalido() throws Exception {
        criarVoucher("TEST-0003");

        mvc.perform(get("/api/v1/vouchers/validate/TEST-0003").with(como("MENTEE", "mentee-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        mvc.perform(post("/api/v1/vouchers/redeem")
                        .with(como("MENTEE", "mentee-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"TEST-0003\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/vouchers/validate/TEST-0003").with(como("MENTEE", "mentee-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }
}
