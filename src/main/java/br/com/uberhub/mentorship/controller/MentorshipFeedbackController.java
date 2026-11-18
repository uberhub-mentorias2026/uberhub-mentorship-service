package br.com.uberhub.mentorship.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.uberhub.mentorship.dto.request.ChangeRequestRequest;
import br.com.uberhub.mentorship.dto.request.FeedbackRequest;
import br.com.uberhub.mentorship.dto.response.ChangeRequestResponse;
import br.com.uberhub.mentorship.dto.response.FeedbackResponse;
import br.com.uberhub.mentorship.service.MentorshipFeedbackService;
import jakarta.validation.Valid;

/**
 * Endpoints de feedback e pedido de troca de mentor.
 *
 * Fica num controller separado do MentorshipController (frente do Manzi) para evitar
 * conflito de merge; os dois usam o mesmo prefixo /api/v1/mentorships, o que o Spring
 * permite desde que os caminhos finais sejam diferentes.
 *
 * Regras gerais (iguais ao VoucherController):
 * - o controller não tem regra de negócio, só recebe, valida (@Valid) e repassa;
 * - o id do usuário vem SEMPRE do token (jwt.getSubject()), nunca do body, para que
 *   ninguém consiga agir em nome de outra pessoa;
 * - @PreAuthorize barra o papel errado com 403 antes de chegar no service.
 */
@RestController
@RequestMapping("/api/v1/mentorships")
public class MentorshipFeedbackController {

    private final MentorshipFeedbackService service;

    public MentorshipFeedbackController(MentorshipFeedbackService service) {
        this.service = service;
    }

    /** Mentor registra o feedback da sessão -> mentoria vira COMPLETED. Retorna 201. */
    @PostMapping("/{id}/feedback/mentor")
    @PreAuthorize("hasRole('MENTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackResponse mentorFeedback(@PathVariable Long id,
                                           @Valid @RequestBody FeedbackRequest request,
                                           @AuthenticationPrincipal Jwt jwt) {
        return service.submitMentorFeedback(id, jwt.getSubject(), request);
    }

    /** Mentorado avalia a mentoria (depois do feedback do mentor). Retorna 201. */
    @PostMapping("/{id}/feedback/mentee")
    @PreAuthorize("hasRole('MENTEE')")
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackResponse menteeFeedback(@PathVariable Long id,
                                           @Valid @RequestBody FeedbackRequest request,
                                           @AuthenticationPrincipal Jwt jwt) {
        return service.submitMenteeFeedback(id, jwt.getSubject(), request);
    }

    /** Mentorado pede troca de mentor -> mentoria vira CHANGE_REQUESTED. Retorna 201. */
    @PostMapping("/{id}/request-change")
    @PreAuthorize("hasRole('MENTEE')")
    @ResponseStatus(HttpStatus.CREATED)
    public ChangeRequestResponse requestChange(@PathVariable Long id,
                                               @Valid @RequestBody ChangeRequestRequest request,
                                               @AuthenticationPrincipal Jwt jwt) {
        return service.requestChange(id, jwt.getSubject(), request);
    }

    /**
     * Interno: lista os pedidos de troca em análise.
     * Chamado pelo admin-service em GET /admin/mentorships/change-requests.
     * (Proposta nossa: a arquitetura cita a chamada, mas não define o endpoint daqui.)
     */
    @GetMapping("/change-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ChangeRequestResponse> listChangeRequests() {
        return service.listPendingChangeRequests();
    }
}
