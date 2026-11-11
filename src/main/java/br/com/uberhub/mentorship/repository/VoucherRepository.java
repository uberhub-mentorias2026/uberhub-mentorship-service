package br.com.uberhub.mentorship.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.uberhub.mentorship.model.Voucher;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * Resgate ATÔMICO: o UPDATE só afeta a linha se o voucher ainda estiver AVAILABLE.
     * Se duas pessoas tentarem resgatar o mesmo código ao mesmo tempo, o banco garante
     * que só uma recebe 1 linha afetada; a outra recebe 0.
     *
     * @return número de linhas alteradas (1 = resgatou, 0 = não existe ou já foi usado)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Voucher v
               set v.status = br.com.uberhub.mentorship.model.VoucherStatus.REDEEMED,
                   v.redeemedBy = :menteeId,
                   v.redeemedAt = :redeemedAt
             where v.code = :code
               and v.status = br.com.uberhub.mentorship.model.VoucherStatus.AVAILABLE
            """)
    int markAsRedeemed(@Param("code") String code,
                       @Param("menteeId") String menteeId,
                       @Param("redeemedAt") Instant redeemedAt);
}
