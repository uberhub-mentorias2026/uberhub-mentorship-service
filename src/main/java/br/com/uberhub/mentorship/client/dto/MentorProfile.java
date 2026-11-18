package br.com.uberhub.mentorship.client.dto;

import java.util.List;

/**
 * Dados do mentor devolvidos pelo profile-service em GET /api/v1/profiles/mentor/{mentorId}.
 *
 * Os nomes seguem a coleção "profiles" da documentação da Equipe B
 * (mentorId, miniBio, areas, schedulingLink, status). Campos que o profile-service
 * mandar a mais são ignorados (o Jackson do Spring não falha com campo desconhecido),
 * então este record pode ficar menor que a resposta real sem quebrar nada.
 *
 * TODO: confirmar com a Equipe B o JSON real (ex.: se existe "name" e se "areas" é lista
 *       de códigos de tag).
 *
 * @param mentorId       id do mentor (mesmo id do auth-service)
 * @param miniBio        mini biografia
 * @param areas          áreas/tags de especialização
 * @param schedulingLink link de agendamento (Calendly / Google Agenda) mostrado ao mentorado
 * @param status         PENDING, APPROVED ou REJECTED
 */
public record MentorProfile(
        String mentorId,
        String miniBio,
        List<String> areas,
        String schedulingLink,
        String status) {
}
