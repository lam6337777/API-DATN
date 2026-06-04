package shoe.com.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shoe.com.api.entity.PaymentEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    List<PaymentEntity> findByOrderId(Long orderId);

    Optional<PaymentEntity> findByVnpayTxnRef(String vnpayTxnRef);
}
