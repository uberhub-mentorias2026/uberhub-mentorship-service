package br.com.uberhub.mentorship.service;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.uberhub.mentorship.dto.response.MentorshipScheduleResponse;
import br.com.uberhub.mentorship.exception.ConflictException;
import br.com.uberhub.mentorship.exception.ForbiddenException;
import br.com.uberhub.mentorship.exception.NotFoundException;
import br.com.uberhub.mentorship.mapper.MentorshipScheduleMapper;
import br.com.uberhub.mentorship.model.Mentorship;
import br.com.uberhub.mentorship.model.MentorshipStatus;
import br.com.uberhub.mentorship.model.Schedule;
import br.com.uberhub.mentorship.repository.MentorshipRepository;

/**
 * Regras de negócio de agendamento e transição de status da mentoria.
 *
 * Transições cobertas aqui:
 * PENDING/SCHEDULED --schedule()--> SCHEDULED
 * SCHEDULED         --confirmSchedule()--> CONFIRMED
 * * --updateStatus(CANCELLED)--> CANCELLED   (exceto a partir de COMPLETED/CANCELLED)
 * CONFIRMED         --updateStatus(COMPLETED)--> COMPLETED
 *
 * PENDING (criação) e CHANGE_REQUESTED (pedido de troca) são responsabilidade de
 * outras frentes (Manzi / Matheus); updateStatus() não aceita esses valores como
 * destino para não duplicar essa lógica.
 */
@Service
public class MentorshipScheduleService {

    /** A partir de qual status é possível (re)agendar uma sessão. */
    private static final Set<MentorshipStatus> SCHEDULABLE_FROM =
            EnumSet.of(MentorshipStatus.PENDING, MentorshipStatus.SCHEDULED);

    /** A partir de qual status é possível cancelar a mentoria via PUT /status. */
    private static final Set<MentorshipStatus> CANCELLABLE_FROM = EnumSet.of(
            MentorshipStatus.PENDING,
            MentorshipStatus.SCHEDULED,
            MentorshipStatus.CONFIRMED,
            MentorshipStatus.CHANGE_REQUESTED);

    private final MentorshipRepository repository;
    private final MentorshipScheduleMapper mapper;

    public MentorshipScheduleService(MentorshipRepository repository, MentorshipScheduleMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /** Mentor propõe (ou reagenda) data/hora da sessão. */
    @Transactional
    public MentorshipScheduleResponse schedule(Long id, Instant dateTime, String mentorId) {
        Mentorship mentorship = find(id);

        if (!mentorship.getMentorId().equals(mentorId)) {
            throw new ForbiddenException("Você não é o mentor desta mentoria");
        }
        if (!SCHEDULABLE_FROM.contains(mentorship.getStatus())) {
            throw new ConflictException(
                    "Não é possível agendar uma mentoria com status " + mentorship.getStatus());
        }

        mentorship.setSchedule(new Schedule(dateTime, false));
        mentorship.setStatus(MentorshipStatus.SCHEDULED);

        return mapper.toResponse(repository.save(mentorship));
    }

    /** Mentorado confirma a data/hora proposta pelo mentor. */
    @Transactional
    public MentorshipScheduleResponse confirmSchedule(Long id, String menteeId) {
        Mentorship mentorship = find(id);

        if (!mentorship.getMenteeId().equals(menteeId)) {
            throw new ForbiddenException("Você não é o mentorado desta mentoria");
        }
        if (mentorship.getStatus() != MentorshipStatus.SCHEDULED) {
            throw new ConflictException(
                    "Mentoria não está aguardando confirmação de agendamento (status atual: "
                            + mentorship.getStatus() + ")");
        }

        mentorship.getSchedule().setConfirmedByMentee(true);
        mentorship.setStatus(MentorshipStatus.CONFIRMED);

        return mapper.toResponse(repository.save(mentorship));
    }

    /**
     * Atualiza o status manualmente. Só aceita CANCELLED ou COMPLETED como destino;
     * SCHEDULED/CONFIRMED passam pelos endpoints dedicados, e PENDING/CHANGE_REQUESTED
     * são definidos por outras frentes.
     */
    @Transactional
    public MentorshipScheduleResponse updateStatus(Long id, MentorshipStatus newStatus, String userId) {
        Mentorship mentorship = find(id);
        requireParticipant(mentorship, userId);

        MentorshipStatus current = mentorship.getStatus();

        switch (newStatus) {
            case CANCELLED -> {
                if (!CANCELLABLE_FROM.contains(current)) {
                    throw new ConflictException(
                            "Não é possível cancelar uma mentoria com status " + current);
                }
            }
            case COMPLETED -> {
                if (current != MentorshipStatus.CONFIRMED) {
                    throw new ConflictException(
                            "Só é possível concluir uma mentoria CONFIRMED (status atual: " + current + ")");
                }
            }
            default -> throw new ConflictException(
                    "PUT /status só aceita CANCELLED ou COMPLETED; use os endpoints de agendamento "
                            + "para SCHEDULED/CONFIRMED");
        }

        mentorship.setStatus(newStatus);
        return mapper.toResponse(repository.save(mentorship));
    }

    private Mentorship find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Mentoria não encontrada"));
    }

    private void requireParticipant(Mentorship mentorship, String userId) {
        if (!userId.equals(mentorship.getMentorId()) && !userId.equals(mentorship.getMenteeId())) {
            throw new ForbiddenException("Você não participa desta mentoria");
        }
    }
}
