package shoe.com.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import shoe.com.api.entity.AddressEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<AddressEntity, Long> {
    List<AddressEntity> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);

    Optional<AddressEntity> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE AddressEntity a SET a.isDefault = false WHERE a.user.id = :userId")
    void clearDefault(@Param("userId") Long userId);
}
