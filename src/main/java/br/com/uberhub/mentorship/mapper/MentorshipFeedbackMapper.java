package br.com.uberhub.mentorship.mapper;

import org.springframework.stereotype.Component;

import br.com.uberhub.mentorship.dto.response.ChangeRequestResponse;
import br.com.uberhub.mentorship.dto.response.FeedbackResponse;
import br.com.uberhub.mentorship.model.ChangeRequest;
import br.com.uberhub.mentorship.model.Feedback;
import br.com.uberhub.mentorship.model.Mentorship;

/**
 * Converte a entidade {@link Mentorship} nos DTOs de resposta da frente de
 * feedback / pedido de troca. Só copia campos; nenhuma regra de negócio aqui.
 */
@Component
public class MentorshipFeedbackMapper {

    /** Monta a resposta a partir da mentoria e de UM dos feedbacks (mentor ou mentee). */
    public FeedbackResponse toFeedbackResponse(Mentorship mentorship, Feedback feedback) {
        return new FeedbackResponse(
                mentorship.getId(),
                mentorship.getStatus(),
                feedback.getRating(),
                feedback.getComment(),
                feedback.getCreatedAt());
    }

    public ChangeRequestResponse toChangeRequestResponse(Mentorship mentorship) {
        // changeRequest pode ser null se a mentoria nunca teve pedido de troca
        ChangeRequest cr = mentorship.getChangeRequest();
        return new ChangeRequestResponse(
                mentorship.getId(),
                mentorship.getMenteeId(),
                mentorship.getMentorId(),
                mentorship.getTopic(),
                mentorship.getStatus(),
                cr != null ? cr.getReason() : null,
                cr != null ? cr.getRequestedAt() : null);
    }
}
