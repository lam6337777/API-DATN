package shoe.com.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import shoe.com.api.entity.VoucherEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<VoucherEntity, Long> {
    Optional<VoucherEntity> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<VoucherEntity> findAllByOrderByCreatedAtDesc();

    @Query("SELECT v FROM VoucherEntity v WHERE v.isActive = true " +
            "AND (v.startAt IS NULL OR v.startAt <= :now) " +
            "AND (v.endAt IS NULL OR v.endAt >= :now) " +
            "AND (v.usageLimit IS NULL OR v.usedCount < v.usageLimit) " +
            "ORDER BY v.minOrderValue ASC")
    List<VoucherEntity> findAvailable(@Param("now") LocalDateTime now);

    /** Tăng lượt dùng nguyên tử: trả về 0 nếu mã đã hết lượt (tránh 2 đơn cùng lúc vượt giới hạn). */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE VoucherEntity v SET v.usedCount = v.usedCount + 1 " +
            "WHERE v.id = :id AND (v.usageLimit IS NULL OR v.usedCount < v.usageLimit)")
    int incrementUsage(@Param("id") Long id);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE VoucherEntity v SET v.usedCount = v.usedCount - 1 WHERE LOWER(v.code) = LOWER(:code) AND v.usedCount > 0")
    int decrementUsage(@Param("code") String code);
}
