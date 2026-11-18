package br.com.uberhub.mentorship.client;

import java.util.Optional;

import br.com.uberhub.mentorship.client.dto.MentorProfile;

/**
 * Porta de comunicação com o profile-service (Equipe B).
 *
 * Usado para obter os dados públicos do mentor (mini bio, áreas e principalmente o
 * link de agendamento), que o mentorado vê depois do match.
 *
 * Retorna Optional vazio quando o perfil não existe OU quando o profile-service não
 * responde: quem chama decide o que fazer (ex.: devolver a mentoria sem o link),
 * em vez de derrubar a requisição inteira.
 *
 * Implementações:
 * - {@link StubProfileClient}: padrão em desenvolvimento, devolve um perfil fictício;
 * - {@link HttpProfileClient}: chama o profile-service (services.profile.stub-enabled=false).
 */
public interface ProfileClient {

    Optional<MentorProfile> findMentorProfile(String mentorId);
}
