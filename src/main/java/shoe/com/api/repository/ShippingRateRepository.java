package shoe.com.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shoe.com.api.entity.ShippingRateEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShippingRateRepository extends JpaRepository<ShippingRateEntity, Long> {
    Optional<ShippingRateEntity> findByProvinceIgnoreCase(String province);

    List<ShippingRateEntity> findAllByOrderByProvinceAsc();
}
