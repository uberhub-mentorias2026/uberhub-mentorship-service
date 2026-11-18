package br.com.uberhub.mentorship.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.uberhub.mentorship.model.Mentorship;
import br.com.uberhub.mentorship.model.MentorshipStatus;

public interface MentorshipRepository extends JpaRepository<Mentorship, Long> {

    List<Mentorship> findByMenteeId(String menteeId);

    List<Mentorship> findByMentorId(String mentorId);

    /**
     * Lista mentorias por status, mais antigas primeiro.
     * Usado para o admin listar os pedidos de troca pendentes (status CHANGE_REQUESTED).
     * O Spring Data gera o SQL a partir do nome do método.
     */
    List<Mentorship> findByStatusOrderByUpdatedAtAsc(MentorshipStatus status);
}
