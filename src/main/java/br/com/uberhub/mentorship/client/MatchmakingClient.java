package br.com.uberhub.mentorship.client;

import java.util.List;

/**
 * Porta de comunicação com o matchmaking-service.
 *
 * TODO (alinhar com o responsável pelo matchmaking): endpoint, JSON de entrada e saída.
 * Enquanto não houver contrato, usa-se o StubMatchmakingClient.
 * Quando o contrato existir, criar uma implementação com Feign/WebClient
 * e desligar o stub (services.matchmaking.stub-enabled=false).
 */
public interface MatchmakingClient {

    /** Retorna o mentorId mais adequado para as tags informadas. */
    String findMentorId(List<String> tags);
}
