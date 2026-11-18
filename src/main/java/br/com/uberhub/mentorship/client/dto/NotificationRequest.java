package br.com.uberhub.mentorship.client.dto;

import java.util.Map;

/**
 * Corpo enviado ao admin-service em POST /api/v1/notifications/send.
 *
 * Segundo a arquitetura, o endpoint "recebe userId e mensagem"; o próprio admin-service
 * busca os tokens FCM no auth-service e dispara o push pelo Firebase. Ou seja, o
 * mentorship-service NÃO precisa conhecer token FCM nenhum.
 *
 * O JSON exato ainda não foi publicado pela Equipe C; este é o formato proposto.
 *
 * Exemplo:
 * <pre>
 * {
 *   "userId": "id-do-mentorado",
 *   "title": "Mentoria concluída",
 *   "message": "Seu mentor registrou o feedback. Que tal avaliar a sessão?",
 *   "data": { "mentorshipId": "10", "type": "MENTOR_FEEDBACK" }
 * }
 * </pre>
 *
 * @param userId  destinatário do push
 * @param title   título da notificação
 * @param message texto da notificação
 * @param data    pares chave/valor extras (o FCM só aceita String), usados pelo app para abrir a tela certa
 */
public record NotificationRequest(
        String userId,
        String title,
        String message,
        Map<String, String> data) {
}
