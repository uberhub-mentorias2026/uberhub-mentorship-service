package br.com.uberhub.mentorship.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.uberhub.mentorship.model.Mentorship;

public interface MentorshipRepository extends JpaRepository<Mentorship, Long> {

    List<Mentorship> findByMenteeId(String menteeId);

    List<Mentorship> findByMentorId(String mentorId);
}
