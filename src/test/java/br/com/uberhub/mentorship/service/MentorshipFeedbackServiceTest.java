package br.com.uberhub.mentorship.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.uberhub.mentorship.client.AdminClient;
import br.com.uberhub.mentorship.client.AnalyticsEventType;
import br.com.uberhub.mentorship.dto.request.ChangeRequestRequest;
import br.com.uberhub.mentorship.dto.request.FeedbackRequest;
import br.com.uberhub.mentorship.dto.response.ChangeRequestResponse;
import br.com.uberhub.mentorship.dto.response.FeedbackResponse;
import br.com.uberhub.mentorship.exception.ConflictException;
import br.com.uberhub.mentorship.exception.ForbiddenException;
import br.com.uberhub.mentorship.exception.NotFoundException;
import br.com.uberhub.mentorship.mapper.MentorshipFeedbackMapper;
import br.com.uberhub.mentorship.model.Feedback;
import br.com.uberhub.mentorship.model.Mentorship;
import br.com.uberhub.mentorship.model.MentorshipStatus;
import br.com.uberhub.mentorship.repository.MentorshipRepository;

/**
 * Testes unitários das regras do MentorshipFeedbackService.
 * Banco e admin-service são mocks (Mockito); não sobe Spring.
 * Sem transação ativa, as chamadas ao admin acontecem na hora, então dá para verificá-las.
 */
@ExtendWith(MockitoExtension.class)
class MentorshipFeedbackServiceTest {

    private static final String MENTOR = "mentor-1";
    private static final String MENTEE = "mentee-1";

    @Mock
    MentorshipRepository repository;

    @Mock
    AdminClient adminClient;

    MentorshipFeedbackService service;

    @BeforeEach
    void setUp() {
        service = new MentorshipFeedbackService(repository, adminClient, new MentorshipFeedbackMapper());
    }

    /** Cria uma mentoria em memória com o status desejado e configura o findById. */
    private Mentorship mentoria(MentorshipStatus status) {
        Mentorship m = Mentorship.builder()
                .id(10L).mentorId(MENTOR).menteeId(MENTEE).topic("Spring").status(status)
                .build();
        when(repository.findById(10L)).thenReturn(Optional.of(m));
        return m;
    }

    // ---------------- feedback do mentor ----------------

    @Test
    void feedbackMentor_sessaoConfirmada_concluiMentoriaENotifica() {
        Mentorship m = mentoria(MentorshipStatus.CONFIRMED);

        FeedbackResponse resp = service.submitMentorFeedback(10L, MENTOR, new FeedbackRequest(5, "  Ótimo  "));

        assertThat(resp.status()).isEqualTo(MentorshipStatus.COMPLETED);
        assertThat(resp.rating()).isEqualTo(5);
        assertThat(resp.comment()).isEqualTo("Ótimo");
        assertThat(m.getMentorFeedback().getCreatedAt()).isNotNull();
        verify(repository).save(m);
        verify(adminClient).recordEvent(eq(AnalyticsEventType.MENTORSHIP_COMPLETED), eq(MENTOR), anyMap());
        verify(adminClient).sendNotification(eq(MENTEE), anyString(), anyString(), anyMap());
    }

    @Test
    void feedbackMentor_outroMentor_retornaForbidden() {
        mentoria(MentorshipStatus.CONFIRMED);

        assertThatThrownBy(() -> service.submitMentorFeedback(10L, "mentor-2", new FeedbackRequest(5, null)))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(adminClient);
    }

    @Test
    void feedbackMentor_sessaoNaoConfirmada_retornaConflict() {
        mentoria(MentorshipStatus.SCHEDULED);

        assertThatThrownBy(() -> service.submitMentorFeedback(10L, MENTOR, new FeedbackRequest(4, null)))
                .isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void feedbackMentor_duplicado_retornaConflict() {
        Mentorship m = mentoria(MentorshipStatus.COMPLETED);
        m.setMentorFeedback(new Feedback(5, null, Instant.now()));

        assertThatThrownBy(() -> service.submitMentorFeedback(10L, MENTOR, new FeedbackRequest(3, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("já foi enviado");
    }

    @Test
    void feedbackMentor_mentoriaInexistente_retornaNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitMentorFeedback(99L, MENTOR, new FeedbackRequest(5, null)))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------------- feedback do mentorado ----------------

    @Test
    void feedbackMentee_aposFeedbackDoMentor_salvaENotificaMentor() {
        Mentorship m = mentoria(MentorshipStatus.COMPLETED);

        FeedbackResponse resp = service.submitMenteeFeedback(10L, MENTEE, new FeedbackRequest(4, "   "));

        assertThat(resp.rating()).isEqualTo(4);
        assertThat(resp.comment()).isNull(); // comentário em branco vira null
        assertThat(m.getStatus()).isEqualTo(MentorshipStatus.COMPLETED); // status não muda
        verify(adminClient).recordEvent(eq(AnalyticsEventType.MENTORSHIP_RATED), eq(MENTEE), anyMap());
        verify(adminClient).sendNotification(eq(MENTOR), anyString(), anyString(), anyMap());
    }

    @Test
    void feedbackMentee_antesDoMentor_retornaConflict() {
        mentoria(MentorshipStatus.CONFIRMED);

        assertThatThrownBy(() -> service.submitMenteeFeedback(10L, MENTEE, new FeedbackRequest(4, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void feedbackMentee_outroMentorado_retornaForbidden() {
        mentoria(MentorshipStatus.COMPLETED);

        assertThatThrownBy(() -> service.submitMenteeFeedback(10L, "mentee-2", new FeedbackRequest(4, null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void feedbackMentee_duplicado_retornaConflict() {
        Mentorship m = mentoria(MentorshipStatus.COMPLETED);
        m.setMenteeFeedback(new Feedback(5, null, Instant.now()));

        assertThatThrownBy(() -> service.submitMenteeFeedback(10L, MENTEE, new FeedbackRequest(4, null)))
                .isInstanceOf(ConflictException.class);
    }

    // ---------------- pedido de troca ----------------

    @Test
    void requestChange_mentoriaEmAndamento_registraPedido() {
        Mentorship m = mentoria(MentorshipStatus.SCHEDULED);

        ChangeRequestResponse resp = service.requestChange(10L, MENTEE, new ChangeRequestRequest("  Horários incompatíveis "));

        assertThat(resp.status()).isEqualTo(MentorshipStatus.CHANGE_REQUESTED);
        assertThat(resp.reason()).isEqualTo("Horários incompatíveis");
        assertThat(resp.requestedAt()).isNotNull();
        assertThat(m.getStatus()).isEqualTo(MentorshipStatus.CHANGE_REQUESTED);
        verify(adminClient).recordEvent(eq(AnalyticsEventType.MENTOR_CHANGE_REQUESTED), eq(MENTEE), anyMap());
    }

    @Test
    void requestChange_pedidoJaEmAberto_retornaConflict() {
        mentoria(MentorshipStatus.CHANGE_REQUESTED);

        assertThatThrownBy(() -> service.requestChange(10L, MENTEE, new ChangeRequestRequest("de novo")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Já existe");
    }

    @Test
    void requestChange_mentoriaConcluida_retornaConflict() {
        mentoria(MentorshipStatus.COMPLETED);

        assertThatThrownBy(() -> service.requestChange(10L, MENTEE, new ChangeRequestRequest("motivo")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void requestChange_outroMentorado_retornaForbidden() {
        mentoria(MentorshipStatus.PENDING);

        assertThatThrownBy(() -> service.requestChange(10L, "mentee-2", new ChangeRequestRequest("motivo")))
                .isInstanceOf(ForbiddenException.class);
    }
}
