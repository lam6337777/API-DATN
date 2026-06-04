package shoe.com.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import shoe.com.api.entity.UserEntity;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    // Thống kê
    long countByIsActiveTrue();

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.createdAt >= :since")
    long countNewUsersSince(@Param("since") java.time.LocalDateTime since);
}
