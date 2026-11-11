package br.com.uberhub.mentorship.model;

import java.time.Instant;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Usado para o feedback do mentor e a avaliação do mentorado. Colunas definidas em Mentorship (@AttributeOverrides). */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {
    private Integer rating;
    private String comment;
    private Instant createdAt;
}
