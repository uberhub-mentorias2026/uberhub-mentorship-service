package br.com.uberhub.mentorship.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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

import br.com.uberhub.mentorship.model.Mentorship;
import br.com.uberhub.mentorship.model.MentorshipStatus;
import br.com.uberhub.mentorship.model.Schedule;
import br.com.uberhub.mentorship.repository.MentorshipRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MentorshipScheduleControllerTest {

    private static final String MENTOR_ID = "mentor-1";
    private static final String MENTEE_ID = "mentee-1";

    @Autowired
    MockMvc mvc;

    @Autowired
    MentorshipRepository repository;

    @BeforeEach
    void limparBanco() {
        repository.deleteAll();
    }

    private static RequestPostProcessor como(String role, String userId) {
        return jwt().jwt(j -> j.subject(userId)).authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private Long criarMentoria(MentorshipStatus status) {
        Mentorship mentorship = Mentorship.builder()
                .mentorId(MENTOR_ID)
                .menteeId(MENTEE_ID)
                .topic("Java")
                .status(status)
                .build();
        return repository.save(mentorship).getId();
    }

    // ---------- PUT /{id}/schedule ----------

    @Test
    void schedule_comoMentorDono_retorna200EAgenda() throws Exception {
        Long id = criarMentoria(MentorshipStatus.PENDING);
        String dataFutura = Instant.now().plus(2, ChronoUnit.DAYS).toString();

        mvc.perform(put("/api/v1/mentorships/" + id + "/schedule")
                        .with(como("MENTOR", MENTOR_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dateTime\":\"" + dataFutura + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.scheduleConfirmedByMentee").value(false));

        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(MentorshipStatus.SCHEDULED);
    }

    @Test
    void schedule_comoOutroMentor_retorna403() throws Exception {
        Long id = criarMentoria(MentorshipStatus.PENDING);
        String dataFutura = Instant.now().plus(2, ChronoUnit.DAYS).toString();

        mvc.perform(put("/api/v1/mentorships/" + id + "/schedule")
                        .with(como("MENTOR", "outro-mentor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dateTime\":\"" + dataFutura + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void schedule_comoMentee_retorna403() throws Exception {
        Long id = criarMentoria(MentorshipStatus.PENDING);
        String dataFutura = Instant.now().plus(2, ChronoUnit.DAYS).toString();

        mvc.perform(put("/api/v1/mentorships/" + id + "/schedule")
                        .with(como("MENTEE", MENTEE_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dateTime\":\"" + dataFutura + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void schedule_dataNoPassado_retorna400() throws Exception {
        Long id = criarMentoria(MentorshipStatus.PENDING);
        String dataPassada = Instant.now().minus(1, ChronoUnit.DAYS).toString();

        mvc.perform(put("/api/v1/mentorships/" + id + "/schedule")
                        .with(como("MENTOR", MENTOR_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dateTime\":\"" + dataPassada + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void schedule_statusConfirmed_retorna409() throws Exception {
        Long id = criarMentoria(MentorshipStatus.CONFIRMED);
        String dataFutura = Instant.now().plus(2, ChronoUnit.DAYS).toString();

        mvc.perform(put("/api/v1/mentorships/" + id + "/schedule")
                        .with(como("MENTOR", MENTOR_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dateTime\":\"" + dataFutura + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void schedule_idInexistente_retorna404() throws Exception {
        String dataFutura = Instant.now().plus(2, ChronoUnit.DAYS).toString();

        mvc.perform(put("/api/v1/mentorships/999999/schedule")
                        .with(como("MENTOR", MENTOR_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dateTime\":\"" + dataFutura + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void schedule_semToken_retorna401() throws Exception {
        Long id = criarMentoria(MentorshipStatus.PENDING);
        String dataFutura = Instant.now().plus(2, ChronoUnit.DAYS).toString();

        mvc.perform(put("/api/v1/mentorships/" + id + "/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dateTime\":\"" + dataFutura + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- POST /{id}/schedule/confirm ----------

    @Test
    void confirmSchedule_comoMenteeDono_retorna200EConfirma() throws Exception {
        Long id = criarMentoria(MentorshipStatus.SCHEDULED);
        Mentorship m = repository.findById(id).orElseThrow();
        m.setSchedule(new Schedule(Instant.now().plus(1, ChronoUnit.DAYS), false));
        repository.save(m);

        mvc.perform(post("/api/v1/mentorships/" + id + "/schedule/confirm")
                        .with(como("MENTEE", MENTEE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.scheduleConfirmedByMentee").value(true));
    }

    @Test
    void confirmSchedule_comoMentor_retorna403() throws Exception {
        Long id = criarMentoria(MentorshipStatus.SCHEDULED);

        mvc.perform(post("/api/v1/mentorships/" + id + "/schedule/confirm")
                        .with(como("MENTOR", MENTOR_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirmSchedule_aindaNaoAgendada_retorna409() throws Exception {
        Long id = criarMentoria(MentorshipStatus.PENDING);

        mvc.perform(post("/api/v1/mentorships/" + id + "/schedule/confirm")
                        .with(como("MENTEE", MENTEE_ID)))
                .andExpect(status().isConflict());
    }

    // ---------- PUT /{id}/status ----------

    @Test
    void updateStatus_cancelarComoMentor_retorna200() throws Exception {
        Long id = criarMentoria(MentorshipStatus.PENDING);

        mvc.perform(put("/api/v1/mentorships/" + id + "/status")
                        .with(como("MENTOR", MENTOR_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void updateStatus_completarAPartirDePending_retorna409() throws Exception {
        Long id = criarMentoria(MentorshipStatus.PENDING);

        mvc.perform(put("/api/v1/mentorships/" + id + "/status")
                        .with(como("MENTOR", MENTOR_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateStatus_usuarioNaoParticipante_retorna403() throws Exception {
        Long id = criarMentoria(MentorshipStatus.PENDING);

        mvc.perform(put("/api/v1/mentorships/" + id + "/status")
                        .with(como("MENTEE", "intruso"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStatus_idInexistente_retorna404() throws Exception {
        mvc.perform(put("/api/v1/mentorships/999999/status")
                        .with(como("MENTOR", MENTOR_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isNotFound());
    }
}
