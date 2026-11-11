package br.com.uberhub.mentorship.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mentorships")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Mentorship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mentor_id", nullable = false, length = 64)
    private String mentorId;

    @Column(name = "mentee_id", nullable = false, length = 64)
    private String menteeId;

    @Column(nullable = false)
    private String topic;

    @ElementCollection
    @CollectionTable(name = "mentorship_tags", joinColumns = @JoinColumn(name = "mentorship_id"))
    @Column(name = "tag", length = 100)
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MentorshipStatus status;

    @Embedded
    private Schedule schedule;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "rating",    column = @Column(name = "mentor_feedback_rating")),
        @AttributeOverride(name = "comment",   column = @Column(name = "mentor_feedback_comment", length = 1000)),
        @AttributeOverride(name = "createdAt", column = @Column(name = "mentor_feedback_at"))
    })
    private Feedback mentorFeedback;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "rating",    column = @Column(name = "mentee_feedback_rating")),
        @AttributeOverride(name = "comment",   column = @Column(name = "mentee_feedback_comment", length = 1000)),
        @AttributeOverride(name = "createdAt", column = @Column(name = "mentee_feedback_at"))
    })
    private Feedback menteeFeedback;

    @Embedded
    private ChangeRequest changeRequest;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
