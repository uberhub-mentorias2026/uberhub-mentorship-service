package br.com.uberhub.mentorship.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import br.com.uberhub.mentorship.client.dto.MentorProfile;

/**
 * Definição HTTP (Feign) do endpoint do profile-service que usamos.
 *
 * URL: propriedade services.profile.url (variável PROFILE_URL). Padrão 8082, conforme
 * documentação da Equipe B. Não use direto nos services: use {@link ProfileClient}.
 */
@FeignClient(
        name = "profile-service",
        url = "${services.profile.url:http://localhost:8082}",
        configuration = FeignAuthForwardingConfig.class)
public interface ProfileServiceApi {

    @GetMapping("/api/v1/profiles/mentor/{mentorId}")
    MentorProfile getMentorProfile(@PathVariable("mentorId") String mentorId);
}
