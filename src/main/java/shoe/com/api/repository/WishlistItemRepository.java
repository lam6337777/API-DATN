package shoe.com.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import shoe.com.api.entity.WishlistItemEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItemEntity, Long> {
    List<WishlistItemEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<WishlistItemEntity> findByUserIdAndProductId(Long userId, String productId);

    @Query("SELECT w.product.id FROM WishlistItemEntity w WHERE w.user.id = :userId")
    List<String> findProductIdsByUserId(@Param("userId") Long userId);
}
