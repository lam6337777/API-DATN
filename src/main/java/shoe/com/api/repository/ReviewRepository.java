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

    /** Lọc đánh giá công khai: theo số sao (null = tất cả) và/hoặc chỉ những đánh giá có bình luận */
    @Query("SELECT r FROM ReviewEntity r WHERE r.product.id = :productId"
            + " AND (:rating = 0 OR r.rating = :rating)"
            + " AND (:withComment = false OR (r.comment IS NOT NULL AND TRIM(r.comment) <> ''))"
            + " ORDER BY r.createdAt DESC")
    Page<ReviewEntity> searchByProduct(@Param("productId") String productId, @Param("rating") int rating,
            @Param("withComment") boolean withComment, Pageable pageable);

    /** Số đánh giá theo từng mức sao: mỗi dòng [rating, count] */
    @Query("SELECT r.rating, COUNT(r) FROM ReviewEntity r WHERE r.product.id = :productId GROUP BY r.rating")
    List<Object[]> countByRating(@Param("productId") String productId);

    @Query("SELECT COUNT(r) FROM ReviewEntity r WHERE r.product.id = :productId AND r.comment IS NOT NULL AND TRIM(r.comment) <> ''")
    long countWithComment(@Param("productId") String productId);
}
