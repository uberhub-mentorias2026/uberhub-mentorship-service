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

    private void criarVoucher(String code, String ownerId) {
        repository.save(Voucher.builder().code(code).status(VoucherStatus.AVAILABLE).ownerId(ownerId).build());
    }

    @Test
    void generate_comoAdminComDono_retorna201ESalvaODono() throws Exception {
        mvc.perform(post("/api/v1/vouchers/generate")
                        .with(como("ADMIN", "admin-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3,\"ownerId\":\"mentor-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[0].code").value(org.hamcrest.Matchers.matchesPattern("[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}")));

        assertThat(repository.count()).isEqualTo(3);
        assertThat(repository.findByOwnerIdOrderByCreatedAtDesc("mentor-1")).hasSize(3);
    }

    @Test
    void generate_semDono_retorna201() throws Exception {
        mvc.perform(post("/api/v1/vouchers/generate")
                        .with(como("ADMIN", "admin-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2));
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
    void listarDoMentor_retornaSomenteOsVouchersDele() throws Exception {
        criarVoucher("AAAA-1111-BBBB", "mentor-1");
        criarVoucher("CCCC-2222-DDDD", "mentor-1");
        criarVoucher("EEEE-3333-FFFF", "mentor-2");

        mvc.perform(get("/api/v1/vouchers/mentor").with(como("MENTOR", "mentor-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void listarDoMentor_comoMentee_retorna403() throws Exception {
        mvc.perform(get("/api/v1/vouchers/mentor").with(como("MENTEE", "mentee-1")))
                .andExpect(status().isForbidden());
    }

    @Test
    void redeem_fluxoCompleto_resgataDepoisRetorna409() throws Exception {
        criarVoucher("TEST-0001-ABCD", "mentor-1");

        mvc.perform(post("/api/v1/vouchers/redeem")
                        .with(como("MENTEE", "mentee-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"test-0001-abcd\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REDEEMED"));

        Voucher salvo = repository.findByCode("TEST-0001-ABCD").orElseThrow();
        assertThat(salvo.getRedeemedBy()).isEqualTo("mentee-1");

        mvc.perform(post("/api/v1/vouchers/redeem")
                        .with(como("MENTEE", "mentee-2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"TEST-0001-ABCD\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void redeem_codigoInexistente_retorna404() throws Exception {
        mvc.perform(post("/api/v1/vouchers/redeem")
                        .with(como("MENTEE", "mentee-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ZZZZ-ZZZZ-ZZZZ\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void redeem_formatoInvalido_retorna400() throws Exception {
        mvc.perform(post("/api/v1/vouchers/redeem")
                        .with(como("MENTEE", "mentee-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ABCD-1234\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void redeem_comoMentor_retorna403() throws Exception {
        criarVoucher("TEST-0002-ABCD", "mentor-1");

        mvc.perform(post("/api/v1/vouchers/redeem")
                        .with(como("MENTOR", "mentor-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"TEST-0002-ABCD\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void redeem_semToken_retorna401() throws Exception {
        mvc.perform(post("/api/v1/vouchers/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"TEST-0001-ABCD\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validate_disponivelDepoisResgatadoFicaInvalido() throws Exception {
        criarVoucher("TEST-0003-ABCD", "mentor-1");

        mvc.perform(get("/api/v1/vouchers/validate/TEST-0003-ABCD").with(como("MENTEE", "mentee-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        mvc.perform(post("/api/v1/vouchers/redeem")
                        .with(como("MENTEE", "mentee-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"TEST-0003-ABCD\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/vouchers/validate/TEST-0003-ABCD").with(como("MENTEE", "mentee-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }
}
