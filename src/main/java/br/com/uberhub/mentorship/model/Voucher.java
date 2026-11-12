package br.com.uberhub.mentorship.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vouchers")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Formato fixo XXXX-XXXX-XXXX (14 caracteres, com hifens). */
    @Column(nullable = false, unique = true, columnDefinition = "CHAR(14)")
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VoucherStatus status;

    /** Mentor a quem o admin liberou o voucher (quem repassa o código ao mentorado). */
    @Column(name = "owner_id", length = 64)
    private String ownerId;

    @Column(name = "redeemed_by", length = 64)
    private String redeemedBy;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
