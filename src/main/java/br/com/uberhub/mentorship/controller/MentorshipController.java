package br.com.uberhub.mentorship.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.uberhub.mentorship.service.MentorshipService;

@RestController
@RequestMapping("/api/v1/mentorships")
public class MentorshipController {

    private final MentorshipService service;

    public MentorshipController(MentorshipService service) {
        this.service = service;
    }

    // TODO: POST /, GET /mentee, GET /mentor, PUT /{id}/status,
    //       PUT /{id}/schedule, POST /{id}/schedule/confirm,
    //       POST /{id}/feedback/mentor, POST /{id}/feedback/mentee, POST /{id}/request-change
}
