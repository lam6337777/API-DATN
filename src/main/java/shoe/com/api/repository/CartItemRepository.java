package shoe.com.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shoe.com.api.entity.CartItemEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItemEntity, Long> {

    List<CartItemEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<CartItemEntity> findByUserIdAndVariantId(Long userId, Long variantId);

    void deleteByUserId(Long userId);

    long countByUserId(Long userId);
}
