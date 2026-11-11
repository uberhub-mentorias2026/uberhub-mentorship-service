package br.com.uberhub.mentorship.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRequest {

    @Column(name = "change_reason", length = 1000)
    private String reason;

    @Column(name = "change_requested_at")
    private Instant requestedAt;
}
