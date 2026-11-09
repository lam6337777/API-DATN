package shoe.com.api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import shoe.com.api.entity.ProductEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, String> {

        @Query("SELECT MAX(COALESCE(p.salePrice, p.originalPrice)) FROM ProductEntity p WHERE p.isActive = true")
        BigDecimal findMaxPrice();

        Optional<ProductEntity> findBySlug(String slug);

        boolean existsBySlug(String slug);

        // Sản phẩm nổi bật
        List<ProductEntity> findByIsFeaturedTrueAndIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

        // Tìm kiếm theo tên
        @Query("SELECT p FROM ProductEntity p WHERE p.isActive = true AND LOWER(p.name) LIKE :keyword")
        Page<ProductEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

        // Dùng EXISTS thay cho JOIN + DISTINCT để có thể sắp xếp theo giá thực tế (COALESCE)
        @Query("SELECT p FROM ProductEntity p LEFT JOIN p.category c LEFT JOIN p.brand b WHERE p.isActive = true " +
                        "AND (:categoryName IS NULL OR c.name = :categoryName) " +
                        "AND (:brandName IS NULL OR b.name = :brandName) " +
                        "AND (:minPrice IS NULL OR COALESCE(p.salePrice, p.originalPrice) >= :minPrice) " +
                        "AND (:maxPrice IS NULL OR COALESCE(p.salePrice, p.originalPrice) <= :maxPrice) " +
                        "AND (:onSale = false OR (p.salePrice IS NOT NULL AND p.salePrice < p.originalPrice)) " +
                        "AND (:productSize IS NULL OR EXISTS (SELECT 1 FROM ProductVariantEntity v " +
                        "     WHERE v.product = p AND v.size = :productSize AND v.stockQuantity > 0)) " +
                        "AND (:keyword IS NULL OR LOWER(p.name) LIKE :keyword OR LOWER(b.name) LIKE :keyword " +
                        "     OR EXISTS (SELECT 1 FROM ProductVariantEntity v2 WHERE v2.product = p AND LOWER(v2.sku) LIKE :keyword))")
        Page<ProductEntity> filterProducts(
                        @Param("categoryName") String categoryName,
                        @Param("brandName") String brandName,
                        @Param("minPrice") BigDecimal minPrice,
                        @Param("maxPrice") BigDecimal maxPrice,
                        @Param("productSize") String productSize,
                        @Param("keyword") String keyword,
                        @Param("onSale") boolean onSale,
                        Pageable pageable);

        // Sản phẩm liên quan: cùng danh mục, bán chạy trước
        @Query("SELECT p FROM ProductEntity p WHERE p.isActive = true AND p.id <> :productId " +
                        "AND p.category.id = :categoryId ORDER BY p.totalSold DESC")
        List<ProductEntity> findRelated(@Param("productId") String productId,
                        @Param("categoryId") String categoryId, Pageable pageable);

        List<ProductEntity> findByIdInAndIsActiveTrue(List<String> ids);

        // Sản phẩm theo danh mục
        Page<ProductEntity> findByCategorySlugAndIsActiveTrue(String categorySlug, Pageable pageable);

        // Sản phẩm theo thương hiệu
        Page<ProductEntity> findByBrandSlugAndIsActiveTrue(String brandSlug, Pageable pageable);

        // Đếm sản phẩm active
        long countByIsActiveTrue();

        // Thống kê tồn kho
        @Query("SELECT COUNT(p) FROM ProductEntity p WHERE (SELECT SUM(v.stockQuantity) FROM ProductVariantEntity v WHERE v.product = p) = 0")
        long countOutOfStock();

        @Query("SELECT COUNT(p) FROM ProductEntity p WHERE (SELECT SUM(v.stockQuantity) FROM ProductVariantEntity v WHERE v.product = p) > 0 "
                        +
                        "AND (SELECT SUM(v.stockQuantity) FROM ProductVariantEntity v WHERE v.product = p) < 10")
        long countLowStock();

        @Query("SELECT SUM(COALESCE(p.salePrice, p.originalPrice) * (SELECT SUM(v.stockQuantity) FROM ProductVariantEntity v WHERE v.product = p)) FROM ProductEntity p")
        BigDecimal calculateTotalInventoryValue();
}
