package br.com.uberhub.mentorship.client;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;

/**
 * Configuração usada pelos Feign clients do admin-service e do profile-service.
 *
 * O que faz: copia o header "Authorization: Bearer ..." da requisição que CHEGOU no
 * mentorship-service para a requisição que SAI para o outro serviço. Assim o
 * profile-service/admin-service recebem o mesmo JWT do usuário e conseguem validá-lo.
 *
 * ATENÇÃO: esta classe NÃO tem @Configuration de propósito. Se tivesse, o interceptor
 * seria aplicado a TODOS os Feign clients do projeto (inclusive um futuro do matchmaking).
 * Ela só vale onde for referenciada em @FeignClient(configuration = ...).
 *
 * Se a chamada não estiver dentro de uma requisição HTTP (ex.: thread de job), não há
 * header para copiar e a chamada sai sem token.
 * TODO: se a Equipe C exigir um token de serviço para os endpoints internos, trocar aqui.
 */
public class FeignAuthForwardingConfig {

    @Bean
    RequestInterceptor authForwardingInterceptor() {
        return template -> {
            // O Spring MVC guarda a requisição atual numa ThreadLocal; recuperamos daqui.
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                String auth = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (auth != null && !auth.isBlank()) {
                    template.header(HttpHeaders.AUTHORIZATION, auth);
                }
            }
        };
    }
}
