package shoe.com.api.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import shoe.com.api.entity.ReviewEntity;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    Page<ReviewEntity> findByProductIdOrderByCreatedAtDesc(String productId, Pageable pageable);

    Page<ReviewEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT r FROM ReviewEntity r WHERE r.order.id = :orderId")
    List<ReviewEntity> findByOrderId(@Param("orderId") Long orderId);

    // Kiểm tra user đã review sản phẩm trong đơn hàng chưa
    @Query("SELECT COUNT(r) > 0 FROM ReviewEntity r WHERE r.user.id = :userId AND r.product.id = :productId AND r.order.id = :orderId")
    boolean existsByUserIdAndProductIdAndOrderId(@Param("userId") Long userId, @Param("productId") String productId,
            @Param("orderId") Long orderId);

    // Tính rating trung bình
    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM ReviewEntity r WHERE r.product.id = :productId")
    Float avgRatingByProductId(@Param("productId") String productId);

    long countByProductId(String productId);
}
