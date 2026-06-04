package shoe.com.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shoe.com.api.entity.ReturnRequestEntity;
import java.util.List;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequestEntity, Long> {
    List<ReturnRequestEntity> findAllByOrderByCreatedAtDesc();
}
