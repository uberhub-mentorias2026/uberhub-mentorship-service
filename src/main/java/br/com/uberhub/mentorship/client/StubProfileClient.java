package br.com.uberhub.mentorship.client;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import br.com.uberhub.mentorship.client.dto.MentorProfile;

/**
 * Implementação FALSA do {@link ProfileClient} para desenvolvimento e testes.
 *
 * Ativa por padrão enquanto o profile-service não está disponível. Devolve sempre um
 * perfil APPROVED fictício com o mentorId pedido, para que as telas do mentorado
 * possam ser testadas sem subir o serviço da Equipe B.
 */
@Component
@ConditionalOnProperty(name = "services.profile.stub-enabled", havingValue = "true", matchIfMissing = true)
public class StubProfileClient implements ProfileClient {

    @Override
    public Optional<MentorProfile> findMentorProfile(String mentorId) {
        return Optional.of(new MentorProfile(
                mentorId,
                "Mentor de exemplo (stub do profile-service)",
                List.of("JAVA", "SPRING"),
                "https://calendly.com/mentor-stub",
                "APPROVED"));
    }
}
