package br.com.uberhub.mentorship.service;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import br.com.uberhub.mentorship.client.AdminClient;
import br.com.uberhub.mentorship.client.AnalyticsEventType;
import br.com.uberhub.mentorship.dto.request.ChangeRequestRequest;
import br.com.uberhub.mentorship.dto.request.FeedbackRequest;
import br.com.uberhub.mentorship.dto.response.ChangeRequestResponse;
import br.com.uberhub.mentorship.dto.response.FeedbackResponse;
import br.com.uberhub.mentorship.exception.ConflictException;
import br.com.uberhub.mentorship.exception.ForbiddenException;
import br.com.uberhub.mentorship.exception.NotFoundException;
import br.com.uberhub.mentorship.mapper.MentorshipFeedbackMapper;
import br.com.uberhub.mentorship.model.ChangeRequest;
import br.com.uberhub.mentorship.model.Feedback;
import br.com.uberhub.mentorship.model.Mentorship;
import br.com.uberhub.mentorship.model.MentorshipStatus;
import br.com.uberhub.mentorship.repository.MentorshipRepository;

/**
 * Regras de negócio do FECHAMENTO do ciclo da mentoria (frente do Matheus, Marco 3):
 * - feedback do mentor  -> conclui a mentoria (CONFIRMED -> COMPLETED);
 * - feedback do mentee  -> avaliação do mentorado, só depois do feedback do mentor;
 * - pedido de troca     -> mentorado pede outro mentor (-> CHANGE_REQUESTED);
 * - listagem dos pedidos de troca para o admin.
 *
 * Padrão de cada operação:
 * 1. busca a mentoria (404 se não existir);
 * 2. confere se o usuário do token é o dono certo (403 se não for);
 * 3. confere status e duplicidade (409 se a ação não for permitida agora);
 * 4. altera a entidade e salva;
 * 5. DEPOIS do commit, avisa o admin-service (analytics + notificação).
 *
 * Por que "depois do commit"? Se a gravação no banco falhar, não queremos ter mandado
 * um push dizendo que a mentoria foi concluída. E como o AdminClient não lança
 * exceção, uma falha no admin-service também nunca desfaz o que foi salvo.
 */
@Service
public class MentorshipFeedbackService {

    /** Status a partir dos quais o mentorado ainda pode pedir troca de mentor. */
    private static final Set<MentorshipStatus> CHANGE_ALLOWED_FROM =
            EnumSet.of(MentorshipStatus.PENDING, MentorshipStatus.SCHEDULED, MentorshipStatus.CONFIRMED);

    private final MentorshipRepository repository;
    private final AdminClient adminClient;
    private final MentorshipFeedbackMapper mapper;

    public MentorshipFeedbackService(MentorshipRepository repository,
                                     AdminClient adminClient,
                                     MentorshipFeedbackMapper mapper) {
        this.repository = repository;
        this.adminClient = adminClient;
        this.mapper = mapper;
    }

    // ------------------------------------------------------------------------------------
    // POST /mentorships/{id}/feedback/mentor
    // ------------------------------------------------------------------------------------

    /**
     * Mentor registra o feedback da sessão. Isso marca a mentoria como COMPLETED.
     *
     * Regras:
     * - só o mentor DA mentoria pode enviar (403);
     * - só um feedback por mentoria (409);
     * - a sessão precisa estar CONFIRMED, ou seja, agendada e confirmada pelo mentorado (409).
     *
     * Efeitos no admin-service (após o commit):
     * - analytics MENTORSHIP_COMPLETED (alimenta o dashboard);
     * - push para o mentorado pedindo que ele avalie a sessão.
     *
     * @param mentorshipId id da mentoria (path)
     * @param mentorId     id do mentor, vindo do token (claim sub) - nunca do body
     */
    @Transactional
    public FeedbackResponse submitMentorFeedback(Long mentorshipId, String mentorId, FeedbackRequest request) {
        Mentorship mentorship = findOrThrow(mentorshipId);

        if (!mentorId.equals(mentorship.getMentorId())) {
            throw new ForbiddenException("Somente o mentor desta mentoria pode enviar o feedback");
        }
        // A checagem de duplicidade vem ANTES da de status: depois do 1º feedback o status
        // já é COMPLETED, e a mensagem "já enviado" é mais útil para o app do que "status inválido".
        if (mentorship.getMentorFeedback() != null) {
            throw new ConflictException("O feedback do mentor já foi enviado para esta mentoria");
        }
        if (mentorship.getStatus() != MentorshipStatus.CONFIRMED) {
            throw new ConflictException(
                    "O feedback só pode ser enviado depois da sessão confirmada (status atual: "
                            + mentorship.getStatus() + ")");
        }

        Feedback feedback = new Feedback(request.rating(), normalizeComment(request.comment()), Instant.now());
        mentorship.setMentorFeedback(feedback);
        mentorship.setStatus(MentorshipStatus.COMPLETED);
        repository.save(mentorship);

        // TODO (frente do Gabriel Mota - "concessão de voucher ao mentor"): pela arquitetura,
        //      o feedback do mentor também dispara a lógica interna de conceder voucher ao
        //      mentor. Quando a regra existir no VoucherService, chamar aqui, por exemplo:
        //      voucherService.grantToMentor(mentorId, mentorshipId);

        Long id = mentorship.getId();
        String menteeId = mentorship.getMenteeId();
        afterCommit(() -> {
            adminClient.recordEvent(AnalyticsEventType.MENTORSHIP_COMPLETED, mentorId, Map.of(
                    "mentorshipId", id,
                    "menteeId", menteeId,
                    "rating", request.rating()));
            adminClient.sendNotification(menteeId,
                    "Mentoria concluída",
                    "Seu mentor registrou o feedback da sessão. Que tal avaliar a mentoria?",
                    Map.of("mentorshipId", String.valueOf(id), "type", "MENTOR_FEEDBACK"));
        });

        return mapper.toFeedbackResponse(mentorship, feedback);
    }

    // ------------------------------------------------------------------------------------
    // POST /mentorships/{id}/feedback/mentee
    // ------------------------------------------------------------------------------------

    /**
     * Mentorado avalia a mentoria (nota + comentário).
     *
     * Regras:
     * - só o mentorado DA mentoria pode avaliar (403);
     * - só uma avaliação por mentoria (409);
     * - a documentação diz que a avaliação depende do feedback do mentor, então a
     *   mentoria precisa estar COMPLETED (409). O status NÃO muda aqui.
     *
     * Efeitos no admin-service (após o commit):
     * - analytics MENTORSHIP_RATED;
     * - push para o mentor avisando que recebeu uma avaliação.
     */
    @Transactional
    public FeedbackResponse submitMenteeFeedback(Long mentorshipId, String menteeId, FeedbackRequest request) {
        Mentorship mentorship = findOrThrow(mentorshipId);

        if (!menteeId.equals(mentorship.getMenteeId())) {
            throw new ForbiddenException("Somente o mentorado desta mentoria pode avaliá-la");
        }
        if (mentorship.getMenteeFeedback() != null) {
            throw new ConflictException("Você já avaliou esta mentoria");
        }
        if (mentorship.getStatus() != MentorshipStatus.COMPLETED) {
            throw new ConflictException(
                    "A avaliação só pode ser feita depois do feedback do mentor (status atual: "
                            + mentorship.getStatus() + ")");
        }

        Feedback feedback = new Feedback(request.rating(), normalizeComment(request.comment()), Instant.now());
        mentorship.setMenteeFeedback(feedback);
        repository.save(mentorship);

        Long id = mentorship.getId();
        String mentorId = mentorship.getMentorId();
        afterCommit(() -> {
            adminClient.recordEvent(AnalyticsEventType.MENTORSHIP_RATED, menteeId, Map.of(
                    "mentorshipId", id,
                    "mentorId", mentorId,
                    "rating", request.rating()));
            adminClient.sendNotification(mentorId,
                    "Nova avaliação",
                    "Seu mentorado avaliou a mentoria com nota " + request.rating() + ".",
                    Map.of("mentorshipId", String.valueOf(id), "type", "MENTEE_FEEDBACK"));
        });

        return mapper.toFeedbackResponse(mentorship, feedback);
    }

    // ------------------------------------------------------------------------------------
    // POST /mentorships/{id}/request-change
    // ------------------------------------------------------------------------------------

    /**
     * Mentorado pede a troca de mentor. O pedido fica aguardando o admin, que aprova
     * pelo admin-service (POST /admin/mentorships/change-requests/{id}/approve), que por
     * sua vez chama PUT /mentorships/{id}/status (frente do Maxsuel).
     *
     * Regras:
     * - só o mentorado DA mentoria pode pedir (403);
     * - não pode haver outro pedido em aberto (409);
     * - só enquanto a mentoria está em andamento: PENDING, SCHEDULED ou CONFIRMED (409).
     *   Depois de COMPLETED ou CANCELLED não faz sentido trocar o mentor.
     *
     * Efeito no admin-service (após o commit): analytics MENTOR_CHANGE_REQUESTED.
     * Não mandamos push ao mentor de propósito: o pedido ainda vai ser analisado.
     */
    @Transactional
    public ChangeRequestResponse requestChange(Long mentorshipId, String menteeId, ChangeRequestRequest request) {
        Mentorship mentorship = findOrThrow(mentorshipId);

        if (!menteeId.equals(mentorship.getMenteeId())) {
            throw new ForbiddenException("Somente o mentorado desta mentoria pode pedir a troca de mentor");
        }
        if (mentorship.getStatus() == MentorshipStatus.CHANGE_REQUESTED) {
            throw new ConflictException("Já existe um pedido de troca em análise para esta mentoria");
        }
        if (!CHANGE_ALLOWED_FROM.contains(mentorship.getStatus())) {
            throw new ConflictException(
                    "Não é possível pedir troca de mentor com a mentoria em " + mentorship.getStatus());
        }

        mentorship.setChangeRequest(new ChangeRequest(request.reason().trim(), Instant.now()));
        mentorship.setStatus(MentorshipStatus.CHANGE_REQUESTED);
        repository.save(mentorship);

        Long id = mentorship.getId();
        String mentorId = mentorship.getMentorId();
        afterCommit(() -> adminClient.recordEvent(AnalyticsEventType.MENTOR_CHANGE_REQUESTED, menteeId, Map.of(
                "mentorshipId", id,
                "mentorId", mentorId)));

        return mapper.toChangeRequestResponse(mentorship);
    }

    // ------------------------------------------------------------------------------------
    // GET /mentorships/change-requests (ADMIN)
    // ------------------------------------------------------------------------------------

    /**
     * Lista os pedidos de troca ainda em análise (status CHANGE_REQUESTED), do mais
     * antigo para o mais novo. Consumido pelo admin-service em
     * GET /admin/mentorships/change-requests.
     */
    @Transactional(readOnly = true)
    public List<ChangeRequestResponse> listPendingChangeRequests() {
        return repository.findByStatusOrderByUpdatedAtAsc(MentorshipStatus.CHANGE_REQUESTED).stream()
                .map(mapper::toChangeRequestResponse)
                .toList();
    }

    // ------------------------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------------------------

    private Mentorship findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Mentoria não encontrada"));
    }

    /** Comentário só com espaços vira null, para não gravar texto vazio no banco. */
    private String normalizeComment(String comment) {
        return (comment == null || comment.isBlank()) ? null : comment.trim();
    }

    /**
     * Executa a ação somente depois que a transação atual for COMMITADA com sucesso.
     * Se não houver transação ativa (ex.: teste unitário sem Spring), executa na hora.
     */
    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
