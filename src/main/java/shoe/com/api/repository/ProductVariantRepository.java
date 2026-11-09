package shoe.com.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import shoe.com.api.entity.ProductVariantEntity;

import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, Long> {

    List<ProductVariantEntity> findByProductId(String productId);

    boolean existsBySku(String sku);

    /**
     * Trừ tồn kho nguyên tử. Trả về 0 nếu không đủ hàng, nên 2 đơn đặt cùng lúc
     * không thể cùng mua món cuối cùng.
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE ProductVariantEntity v SET v.stockQuantity = v.stockQuantity - :qty " +
            "WHERE v.id = :id AND v.stockQuantity >= :qty")
    int decrementStock(@Param("id") Long id, @Param("qty") int qty);
}
