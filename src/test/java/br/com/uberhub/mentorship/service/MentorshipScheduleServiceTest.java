package br.com.uberhub.mentorship.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.uberhub.mentorship.dto.response.MentorshipScheduleResponse;
import br.com.uberhub.mentorship.exception.ConflictException;
import br.com.uberhub.mentorship.exception.ForbiddenException;
import br.com.uberhub.mentorship.exception.NotFoundException;
import br.com.uberhub.mentorship.mapper.MentorshipScheduleMapper;
import br.com.uberhub.mentorship.model.Mentorship;
import br.com.uberhub.mentorship.model.MentorshipStatus;
import br.com.uberhub.mentorship.model.Schedule;
import br.com.uberhub.mentorship.repository.MentorshipRepository;

@ExtendWith(MockitoExtension.class)
class MentorshipScheduleServiceTest {

    private static final Long ID = 1L;
    private static final String MENTOR_ID = "mentor-1";
    private static final String MENTEE_ID = "mentee-1";

    @Mock
    MentorshipRepository repository;

    MentorshipScheduleService service;

    private static Instant amanha() {
        return Instant.now().plus(1, ChronoUnit.DAYS);
    }

    private static Mentorship mentorship(MentorshipStatus status) {
        return Mentorship.builder()
                .id(ID)
                .mentorId(MENTOR_ID)
                .menteeId(MENTEE_ID)
                .topic("Java")
                .status(status)
                .build();
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new MentorshipScheduleService(repository, new MentorshipScheduleMapper());
    }

    private void stubFindAndSave(Mentorship mentorship) {
        when(repository.findById(ID)).thenReturn(Optional.of(mentorship));
        when(repository.save(any(Mentorship.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- schedule() ----------

    @Test
    void schedule_mentorDonoEStatusPending_agendaERetornaScheduled() {
        Mentorship mentorship = mentorship(MentorshipStatus.PENDING);
        stubFindAndSave(mentorship);
        Instant dateTime = amanha();

        MentorshipScheduleResponse response = service.schedule(ID, dateTime, MENTOR_ID);

        assertThat(response.status()).isEqualTo(MentorshipStatus.SCHEDULED);
        assertThat(response.scheduleDateTime()).isEqualTo(dateTime);
        assertThat(response.scheduleConfirmedByMentee()).isFalse();
    }

    @Test
    void schedule_reagendarAPartirDeScheduled_continuaScheduledComNovaData() {
        Mentorship mentorship = mentorship(MentorshipStatus.SCHEDULED);
        mentorship.setSchedule(new Schedule(Instant.now(), true));
        stubFindAndSave(mentorship);
        Instant novaData = amanha();

        MentorshipScheduleResponse response = service.schedule(ID, novaData, MENTOR_ID);

        assertThat(response.status()).isEqualTo(MentorshipStatus.SCHEDULED);
        assertThat(response.scheduleDateTime()).isEqualTo(novaData);
        assertThat(response.scheduleConfirmedByMentee())
                .as("reagendar deve zerar a confirmação anterior do mentorado")
                .isFalse();
    }

    @Test
    void schedule_naoEhOMentor_lancaForbidden() {
        when(repository.findById(ID)).thenReturn(Optional.of(mentorship(MentorshipStatus.PENDING)));

        assertThatThrownBy(() -> service.schedule(ID, amanha(), "outro-mentor"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void schedule_statusConfirmed_lancaConflict() {
        when(repository.findById(ID)).thenReturn(Optional.of(mentorship(MentorshipStatus.CONFIRMED)));

        assertThatThrownBy(() -> service.schedule(ID, amanha(), MENTOR_ID))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void schedule_idInexistente_lancaNotFound() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.schedule(ID, amanha(), MENTOR_ID))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- confirmSchedule() ----------

    @Test
    void confirmSchedule_menteeDonoEStatusScheduled_confirmaERetornaConfirmed() {
        Mentorship mentorship = mentorship(MentorshipStatus.SCHEDULED);
        mentorship.setSchedule(new Schedule(amanha(), false));
        stubFindAndSave(mentorship);

        MentorshipScheduleResponse response = service.confirmSchedule(ID, MENTEE_ID);

        assertThat(response.status()).isEqualTo(MentorshipStatus.CONFIRMED);
        assertThat(response.scheduleConfirmedByMentee()).isTrue();
    }

    @Test
    void confirmSchedule_naoEhOMentorado_lancaForbidden() {
        Mentorship mentorship = mentorship(MentorshipStatus.SCHEDULED);
        mentorship.setSchedule(new Schedule(amanha(), false));
        when(repository.findById(ID)).thenReturn(Optional.of(mentorship));

        assertThatThrownBy(() -> service.confirmSchedule(ID, "outro-mentee"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void confirmSchedule_aindaNaoAgendada_lancaConflict() {
        when(repository.findById(ID)).thenReturn(Optional.of(mentorship(MentorshipStatus.PENDING)));

        assertThatThrownBy(() -> service.confirmSchedule(ID, MENTEE_ID))
                .isInstanceOf(ConflictException.class);
    }

    // ---------- updateStatus() ----------

    @Test
    void updateStatus_cancelarAPartirDePending_ok() {
        stubFindAndSave(mentorship(MentorshipStatus.PENDING));

        MentorshipScheduleResponse response = service.updateStatus(ID, MentorshipStatus.CANCELLED, MENTOR_ID);

        assertThat(response.status()).isEqualTo(MentorshipStatus.CANCELLED);
    }

    @Test
    void updateStatus_cancelarPeloMentorado_tambemOk() {
        stubFindAndSave(mentorship(MentorshipStatus.SCHEDULED));

        MentorshipScheduleResponse response = service.updateStatus(ID, MentorshipStatus.CANCELLED, MENTEE_ID);

        assertThat(response.status()).isEqualTo(MentorshipStatus.CANCELLED);
    }

    @Test
    void updateStatus_cancelarAPartirDeCompleted_lancaConflict() {
        when(repository.findById(ID)).thenReturn(Optional.of(mentorship(MentorshipStatus.COMPLETED)));

        assertThatThrownBy(() -> service.updateStatus(ID, MentorshipStatus.CANCELLED, MENTOR_ID))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateStatus_completarAPartirDeConfirmed_ok() {
        stubFindAndSave(mentorship(MentorshipStatus.CONFIRMED));

        MentorshipScheduleResponse response = service.updateStatus(ID, MentorshipStatus.COMPLETED, MENTOR_ID);

        assertThat(response.status()).isEqualTo(MentorshipStatus.COMPLETED);
    }

    @Test
    void updateStatus_completarAPartirDePending_lancaConflict() {
        when(repository.findById(ID)).thenReturn(Optional.of(mentorship(MentorshipStatus.PENDING)));

        assertThatThrownBy(() -> service.updateStatus(ID, MentorshipStatus.COMPLETED, MENTOR_ID))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateStatus_destinoScheduled_lancaConflictPoisTemEndpointProprio() {
        when(repository.findById(ID)).thenReturn(Optional.of(mentorship(MentorshipStatus.PENDING)));

        assertThatThrownBy(() -> service.updateStatus(ID, MentorshipStatus.SCHEDULED, MENTOR_ID))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateStatus_usuarioNaoParticipante_lancaForbidden() {
        when(repository.findById(ID)).thenReturn(Optional.of(mentorship(MentorshipStatus.PENDING)));

        assertThatThrownBy(() -> service.updateStatus(ID, MentorshipStatus.CANCELLED, "intruso"))
                .isInstanceOf(ForbiddenException.class);
    }
}
