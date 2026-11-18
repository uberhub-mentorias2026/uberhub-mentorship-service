package br.com.uberhub.mentorship.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.uberhub.mentorship.dto.request.ScheduleMentorshipRequest;
import br.com.uberhub.mentorship.dto.request.UpdateMentorshipStatusRequest;
import br.com.uberhub.mentorship.dto.response.MentorshipScheduleResponse;
import br.com.uberhub.mentorship.service.MentorshipScheduleService;
import jakarta.validation.Valid;

/**
 * Endpoints de agendamento e status da mentoria. Compartilha o mesmo
 * @RequestMapping do MentorshipController (do Manzi), mas em arquivo próprio
 * (ver DIVISAO-DE-TAREFAS.md) pra evitar conflito de merge.
 */
@RestController
@RequestMapping("/api/v1/mentorships")
public class MentorshipScheduleController {

    private final MentorshipScheduleService service;

    public MentorshipScheduleController(MentorshipScheduleService service) {
        this.service = service;
    }

    /** Mentor propõe (ou reagenda) a data/hora da sessão. */
    @PutMapping("/{id}/schedule")
    @PreAuthorize("hasRole('MENTOR')")
    public MentorshipScheduleResponse schedule(@PathVariable Long id,
                                                @Valid @RequestBody ScheduleMentorshipRequest request,
                                                @AuthenticationPrincipal Jwt jwt) {
        return service.schedule(id, request.dateTime(), jwt.getSubject());
    }

    /** Mentorado confirma o agendamento proposto pelo mentor. */
    @PostMapping("/{id}/schedule/confirm")
    @PreAuthorize("hasRole('MENTEE')")
    public MentorshipScheduleResponse confirmSchedule(@PathVariable Long id,
                                                        @AuthenticationPrincipal Jwt jwt) {
        return service.confirmSchedule(id, jwt.getSubject());
    }

    /** Atualiza o status da mentoria (CANCELLED ou COMPLETED). Mentor ou mentorado da mentoria. */
    @PutMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public MentorshipScheduleResponse updateStatus(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateMentorshipStatusRequest request,
                                                     @AuthenticationPrincipal Jwt jwt) {
        return service.updateStatus(id, request.status(), jwt.getSubject());
    }
}
