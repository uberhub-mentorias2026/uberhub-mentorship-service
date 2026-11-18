package br.com.uberhub.mentorship.client;

/**
 * Tipos de evento de negócio que o mentorship-service envia ao admin-service
 * (POST /api/v1/analytics/record-event).
 *
 * O admin-service usa esses eventos para montar o dashboard (GET /admin/dashboard).
 *
 * Origem dos nomes:
 * - MENTORSHIP_COMPLETED e VOUCHER_REDEEMED estão na documentação oficial (coleção "analytics"
 *   do admin-service, campo eventType).
 * - MENTORSHIP_RATED e MENTOR_CHANGE_REQUESTED são PROPOSTAS nossas (não existem na doc).
 *   TODO: confirmar com a Equipe C se aceitam esses nomes.
 */
public enum AnalyticsEventType {

    /** Mentor enviou o feedback -> sessão considerada concluída. */
    MENTORSHIP_COMPLETED,

    /** Mentorado avaliou a mentoria (nota + comentário). */
    MENTORSHIP_RATED,

    /** Mentorado pediu troca de mentor. */
    MENTOR_CHANGE_REQUESTED,

    /** Mentorado resgatou um voucher (usado pela frente de vouchers). */
    VOUCHER_REDEEMED
}
