package shoe.com.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shoe.com.api.entity.BrandEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<BrandEntity, Long> {

    Optional<BrandEntity> findBySlug(String slug);

    Optional<BrandEntity> findByName(String name);

    List<BrandEntity> findByIsActiveTrue();

    boolean existsByName(String name);

    boolean existsBySlug(String slug);
}
