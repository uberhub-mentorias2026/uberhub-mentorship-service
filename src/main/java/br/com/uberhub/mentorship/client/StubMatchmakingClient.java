package br.com.uberhub.mentorship.client;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Implementação falsa para desenvolvimento enquanto o matchmaking não está pronto. */
@Component
@ConditionalOnProperty(name = "services.matchmaking.stub-enabled", havingValue = "true", matchIfMissing = true)
public class StubMatchmakingClient implements MatchmakingClient {

    @Override
    public String findMentorId(List<String> tags) {
        return "mentor-stub-001";
    }
}
