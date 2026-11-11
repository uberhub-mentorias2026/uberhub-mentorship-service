package br.com.uberhub.mentorship.service;

import org.springframework.stereotype.Service;

import br.com.uberhub.mentorship.client.MatchmakingClient;
import br.com.uberhub.mentorship.repository.MentorshipRepository;

@Service
public class MentorshipService {

    private final MentorshipRepository repository;
    private final MatchmakingClient matchmakingClient;

    public MentorshipService(MentorshipRepository repository, MatchmakingClient matchmakingClient) {
        this.repository = repository;
        this.matchmakingClient = matchmakingClient;
    }

    // TODO: create(menteeId, request), listByMentee, listByMentor,
    //       schedule, confirmSchedule, feedback..., requestChange, updateStatus
}
