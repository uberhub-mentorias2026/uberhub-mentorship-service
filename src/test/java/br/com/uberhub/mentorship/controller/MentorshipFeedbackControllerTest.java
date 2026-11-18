package br.com.uberhub.mentorship.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import br.com.uberhub.mentorship.client.AdminClient;
import br.com.uberhub.mentorship.client.AnalyticsEventType;
import br.com.uberhub.mentorship.model.Mentorship;
import br.com.uberhub.mentorship.model.MentorshipStatus;
import br.com.uberhub.mentorship.repository.MentorshipRepository;

/**
 * Teste de integração: sobe o Spring inteiro com H2 (perfil "test") e chama os
 * endpoints via MockMvc, passando por segurança (JWT), validação, service e banco.
 * O AdminClient é trocado por um mock para conferir que o admin-service seria chamado
 * (e só DEPOIS do commit, já que aqui há transação de verdade).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MentorshipFeedbackControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    MentorshipRepository repository;

    @MockBean
    AdminClient adminClient;

    @BeforeEach
    void limparBanco() {
        repository.deleteAll();
    }

    /** Simula um usuário autenticado com o papel e o id (claim sub) informados. */
    private static RequestPostProcessor como(String role, String userId) {
        return jwt().jwt(j -> j.subject(userId)).authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private Long criarMentoria(MentorshipStatus status) {
        return repository.save(Mentorship.builder()
                .mentorId("mentor-1").menteeId("mentee-1").topic("Spring Boot").status(status)
                .build()).getId();
    }

    @Test
    void cicloCompleto_feedbackMentorDepoisMentee() throws Exception {
        Long id = criarMentoria(MentorshipStatus.CONFIRMED);

        // 1) mentor envia feedback -> 201, mentoria COMPLETED
        mvc.perform(post("/api/v1/mentorships/{id}/feedback/mentor", id)
                        .with(como("MENTOR", "mentor-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"Mentorado compareceu e participou\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mentorshipId").value(id))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.rating").value(5));

        verify(adminClient).recordEvent(eq(AnalyticsEventType.MENTORSHIP_COMPLETED), eq("mentor-1"), anyMap());
        verify(adminClient).sendNotification(eq("mentee-1"), anyString(), anyString(), anyMap());

        // 2) segundo feedback do mentor -> 409
        mvc.perform(post("/api/v1/mentorships/{id}/feedback/mentor", id)
                        .with(como("MENTOR", "mentor-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4}"))
                .andExpect(status().isConflict());

        // 3) mentorado avalia -> 201
        mvc.perform(post("/api/v1/mentorships/{id}/feedback/mentee", id)
                        .with(como("MENTEE", "mentee-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4,\"comment\":\"Ajudou bastante\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(4));

        Mentorship salva = repository.findById(id).orElseThrow();
        assertThat(salva.getMentorFeedback().getRating()).isEqualTo(5);
        assertThat(salva.getMenteeFeedback().getComment()).isEqualTo("Ajudou bastante");
        verify(adminClient).recordEvent(eq(AnalyticsEventType.MENTORSHIP_RATED), eq("mentee-1"), anyMap());
    }

    @Test
    void feedbackMentee_antesDoMentor_retorna409() throws Exception {
        Long id = criarMentoria(MentorshipStatus.CONFIRMED);

        mvc.perform(post("/api/v1/mentorships/{id}/feedback/mentee", id)
                        .with(como("MENTEE", "mentee-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4}"))
                .andExpect(status().isConflict());
    }

    @Test
    void feedbackMentor_notaForaDaFaixa_retorna400() throws Exception {
        Long id = criarMentoria(MentorshipStatus.CONFIRMED);

        mvc.perform(post("/api/v1/mentorships/{id}/feedback/mentor", id)
                        .with(como("MENTOR", "mentor-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.startsWith("rating")));
    }

    @Test
    void feedbackMentor_comoMentee_retorna403() throws Exception {
        Long id = criarMentoria(MentorshipStatus.CONFIRMED);

        mvc.perform(post("/api/v1/mentorships/{id}/feedback/mentor", id)
                        .with(como("MENTEE", "mentee-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void feedbackMentor_mentorDeOutraMentoria_retorna403() throws Exception {
        Long id = criarMentoria(MentorshipStatus.CONFIRMED);

        mvc.perform(post("/api/v1/mentorships/{id}/feedback/mentor", id)
                        .with(como("MENTOR", "mentor-2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void feedbackMentor_mentoriaInexistente_retorna404() throws Exception {
        mvc.perform(post("/api/v1/mentorships/{id}/feedback/mentor", 9999)
                        .with(como("MENTOR", "mentor-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void feedback_semToken_retorna401() throws Exception {
        mvc.perform(post("/api/v1/mentorships/{id}/feedback/mentor", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestChange_eListagemDoAdmin() throws Exception {
        Long id = criarMentoria(MentorshipStatus.SCHEDULED);
        criarMentoria(MentorshipStatus.PENDING); // outra mentoria, sem pedido de troca

        mvc.perform(post("/api/v1/mentorships/{id}/request-change", id)
                        .with(como("MENTEE", "mentee-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Horários incompatíveis\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CHANGE_REQUESTED"))
                .andExpect(jsonPath("$.reason").value("Horários incompatíveis"));

        // pedir de novo -> 409
        mvc.perform(post("/api/v1/mentorships/{id}/request-change", id)
                        .with(como("MENTEE", "mentee-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"de novo\"}"))
                .andExpect(status().isConflict());

        // admin lista só o pedido em aberto
        mvc.perform(get("/api/v1/mentorships/change-requests").with(como("ADMIN", "admin-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].mentorshipId").value(id))
                .andExpect(jsonPath("$[0].mentorId").value("mentor-1"));
    }

    @Test
    void requestChange_semMotivo_retorna400() throws Exception {
        Long id = criarMentoria(MentorshipStatus.PENDING);

        mvc.perform(post("/api/v1/mentorships/{id}/request-change", id)
                        .with(como("MENTEE", "mentee-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarPedidosDeTroca_comoMentee_retorna403() throws Exception {
        mvc.perform(get("/api/v1/mentorships/change-requests").with(como("MENTEE", "mentee-1")))
                .andExpect(status().isForbidden());
    }
}
