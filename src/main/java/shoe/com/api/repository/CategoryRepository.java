package shoe.com.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shoe.com.api.entity.CategoryEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, String> {

    Optional<CategoryEntity> findBySlug(String slug);

    Optional<CategoryEntity> findByName(String name);

    List<CategoryEntity> findByIsActiveTrueOrderBySortOrderAsc();

    boolean existsByName(String name);

    boolean existsBySlug(String slug);
}
