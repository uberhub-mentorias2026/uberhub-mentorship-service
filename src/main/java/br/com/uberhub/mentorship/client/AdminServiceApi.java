package br.com.uberhub.mentorship.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import br.com.uberhub.mentorship.client.dto.AnalyticsEventRequest;
import br.com.uberhub.mentorship.client.dto.NotificationRequest;

/**
 * Definição HTTP (Feign) dos endpoints internos do admin-service.
 *
 * O Spring Cloud OpenFeign gera a implementação desta interface em tempo de execução:
 * cada método vira uma chamada HTTP para a URL configurada.
 *
 * URL: propriedade services.admin.url (variável ADMIN_URL). O padrão 8085 vem da
 * documentação do admin-service. Não use esta interface direto nos services:
 * use {@link AdminClient}, que trata as falhas.
 */
@FeignClient(
        name = "admin-service",
        url = "${services.admin.url:http://localhost:8085}",
        configuration = FeignAuthForwardingConfig.class)
public interface AdminServiceApi {

    @PostMapping("/api/v1/analytics/record-event")
    void recordEvent(@RequestBody AnalyticsEventRequest request);

    @PostMapping("/api/v1/notifications/send")
    void sendNotification(@RequestBody NotificationRequest request);
}
