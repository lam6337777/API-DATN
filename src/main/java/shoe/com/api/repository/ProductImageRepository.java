package shoe.com.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shoe.com.api.entity.ProductImageEntity;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImageEntity, Long> {

    List<ProductImageEntity> findByProductIdOrderBySortOrderAsc(String productId);

    void deleteByProductId(String productId);
}
