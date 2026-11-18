package br.com.uberhub.mentorship.client;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import br.com.uberhub.mentorship.client.dto.MentorProfile;
import feign.FeignException;

/**
 * Implementação REAL do {@link ProfileClient}: chama o profile-service via Feign.
 *
 * Só é criada quando services.profile.stub-enabled=false (variável PROFILE_STUB=false).
 *
 * Tratamento de erros:
 * - 404 do profile-service -> mentor não tem perfil -> Optional.empty();
 * - qualquer outra falha (fora do ar, timeout, 5xx) -> loga WARN e Optional.empty(),
 *   para que a tela do mentorado continue funcionando, só sem os dados do mentor.
 */
@Component
@ConditionalOnProperty(name = "services.profile.stub-enabled", havingValue = "false")
public class HttpProfileClient implements ProfileClient {

    private static final Logger log = LoggerFactory.getLogger(HttpProfileClient.class);

    private final ProfileServiceApi api;

    public HttpProfileClient(ProfileServiceApi api) {
        this.api = api;
    }

    @Override
    public Optional<MentorProfile> findMentorProfile(String mentorId) {
        try {
            return Optional.ofNullable(api.getMentorProfile(mentorId));
        } catch (FeignException.NotFound ex) {
            return Optional.empty();
        } catch (RuntimeException ex) {
            log.warn("Falha ao buscar perfil do mentor {} no profile-service: {}", mentorId, ex.getMessage());
            return Optional.empty();
        }
    }
}
