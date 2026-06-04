package shoe.com.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shoe.com.api.dto.request.ProductRequest;
import shoe.com.api.dto.response.ProductFiltersResponse;
import shoe.com.api.dto.response.ProductResponse;
import shoe.com.api.entity.*;
import shoe.com.api.exception.BadRequestException;
import shoe.com.api.exception.ResourceNotFoundException;
import shoe.com.api.repository.*;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final OrderItemRepository orderItemRepository;

    public ProductFiltersResponse getProductFilters() {
        List<String> categories = categoryRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream().map(CategoryEntity::getName).collect(Collectors.toList());
        List<String> brands = brandRepository.findByIsActiveTrue()
                .stream().map(BrandEntity::getName).collect(Collectors.toList());

        BigDecimal maxPrice = productRepository.findMaxPrice();

        return ProductFiltersResponse.builder()
                .categories(categories)
                .brands(brands)
                .maxPrice(maxPrice != null ? maxPrice : BigDecimal.ZERO)
                .build();
    }

    /**
     * Lấy danh sách sản phẩm với filter và pagination
     */
    public Page<ProductResponse> getProducts(String categoryName, String brandName,
            BigDecimal minPrice, BigDecimal maxPrice, String shoeSize, String keyword,
            String sortBy, int page, int size) {
        Sort sort = switch (sortBy != null ? sortBy : "newest") {
            case "price_asc" -> Sort.by("originalPrice").ascending();
            case "price_desc" -> Sort.by("originalPrice").descending();
            case "best_selling" -> Sort.by("totalSold").descending();
            case "rating" -> Sort.by("avgRating").descending();
            default -> Sort.by("createdAt").descending();
        };
        if (sortBy == null || sortBy.equals("newest")) {
            sort = Sort.by("createdAt").descending();
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        String searchKeyword = null;
        if (keyword != null && !keyword.trim().isEmpty()) {
            searchKeyword = "%" + keyword.toLowerCase().trim() + "%";
        }

        Page<ProductEntity> products = productRepository.filterProducts(categoryName, brandName, minPrice, maxPrice,
                shoeSize, searchKeyword, pageable);
        return products.map(this::toProductResponse);
    }

    /**
     * Lấy chi tiết sản phẩm theo ID
     */
    public ProductResponse getProductById(String id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", id));
        return toProductResponse(product);
    }

    /**
     * Tìm kiếm sản phẩm
     */
    public Page<ProductResponse> searchProducts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        String searchKeyword = (keyword != null) ? "%" + keyword.toLowerCase().trim() + "%" : "%%";
        Page<ProductEntity> products = productRepository.searchByKeyword(searchKeyword, pageable);
        return products.map(this::toProductResponse);
    }

    /**
     * Sản phẩm nổi bật
     */
    public List<ProductResponse> getFeaturedProducts(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.findByIsFeaturedTrueAndIsActiveTrueOrderByCreatedAtDesc(pageable)
                .stream().map(this::toProductResponse).collect(Collectors.toList());
    }

    /**
     * Tạo sản phẩm mới (Admin)
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        String slug = generateSlug(request.getName());

        ProductEntity product = ProductEntity.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .originalPrice(request.getOriginalPrice())
                .salePrice(request.getSalePrice())
                .isFeatured(request.getIsFeatured() != null && request.getIsFeatured())
                .build();

        // Gắn danh mục
        if (request.getCategoryId() != null) {
            CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "id", request.getCategoryId()));
            product.setCategory(category);
        }

        // Gắn thương hiệu
        if (request.getBrandId() != null) {
            BrandEntity brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Thương hiệu", "id", request.getBrandId()));
            product.setBrand(brand);
        }

        product = productRepository.save(product);

        // Thêm ảnh
        if (request.getImageUrls() != null) {
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                ProductImageEntity image = ProductImageEntity.builder()
                        .product(product)
                        .imageUrl(request.getImageUrls().get(i))
                        .sortOrder(i)
                        .isPrimary(i == 0) // Ảnh đầu tiên là ảnh chính
                        .build();
                productImageRepository.save(image);
            }
        }

        // Thêm biến thể
        if (request.getVariants() != null) {
            for (ProductRequest.VariantRequest vr : request.getVariants()) {
                // Dùng isBlank để tránh SKU rỗng gây duplicate key
                String sku = (vr.getSku() != null && !vr.getSku().isBlank())
                        ? vr.getSku()
                        : generateSku(product.getId(), vr.getSize());
                ProductVariantEntity variant = ProductVariantEntity.builder()
                        .product(product)
                        .size(vr.getSize())
                        .stockQuantity(vr.getStockQuantity() != null ? vr.getStockQuantity() : 0)
                        .sku(sku)
                        .build();
                productVariantRepository.save(variant);
            }
        }

        return getProductById(product.getId());
    }

    /**
     * Cập nhật sản phẩm (Admin)
     */
    @Transactional
    public ProductResponse updateProduct(String id, ProductRequest request) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", id));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setOriginalPrice(request.getOriginalPrice());
        product.setSalePrice(request.getSalePrice());
        product.setIsFeatured(request.getIsFeatured() != null && request.getIsFeatured());

        if (request.getCategoryId() != null) {
            CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "id", request.getCategoryId()));
            product.setCategory(category);
        }

        if (request.getBrandId() != null) {
            BrandEntity brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Thương hiệu", "id", request.getBrandId()));
            product.setBrand(brand);
        }

        // Cập nhật ảnh bằng cách thao tác trực tiếp trên collection để tránh duplicate
        if (request.getImageUrls() != null) {
            product.getImages().clear();
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                ProductImageEntity image = ProductImageEntity.builder()
                        .product(product)
                        .imageUrl(request.getImageUrls().get(i))
                        .sortOrder(i)
                        .isPrimary(i == 0)
                        .build();
                product.getImages().add(image);
            }
        }

        // Cập nhật variants thông minh: đồng bộ thay vì xóa hết
        if (request.getVariants() != null) {
            List<ProductVariantEntity> currentVariants = product.getVariants();
            List<Long> requestVariantIds = request.getVariants().stream()
                    .map(ProductRequest.VariantRequest::getId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());

            // 1. Xóa các biến thể không còn trong request
            currentVariants.removeIf(v -> {
                if (!requestVariantIds.contains(v.getId())) {
                    if (orderItemRepository.existsByVariantId(v.getId())) {
                        throw new BadRequestException("Không thể xoá biến thể " + v.getSku() + " vì đã có trong đơn hàng");
                    }
                    return true;
                }
                return false;
            });

            // 2. Cập nhật hoặc thêm mới
            for (ProductRequest.VariantRequest vr : request.getVariants()) {
                if (vr.getId() != null) {
                    // Cập nhật biến thể cũ
                    currentVariants.stream()
                            .filter(v -> v.getId().equals(vr.getId()))
                            .findFirst()
                            .ifPresent(v -> {
                                v.setSize(vr.getSize());
                                v.setStockQuantity(vr.getStockQuantity() != null ? vr.getStockQuantity() : 0);
                                // Chỉ cập nhật SKU nếu không rỗng
                                if (vr.getSku() != null && !vr.getSku().isBlank())
                                    v.setSku(vr.getSku());
                            });
                } else {
                    // Thêm biến thể mới — tự generate SKU nếu không có hoặc rỗng
                    String sku = (vr.getSku() != null && !vr.getSku().isBlank())
                            ? vr.getSku()
                            : generateSku(id, vr.getSize());
                    ProductVariantEntity newVariant = ProductVariantEntity.builder()
                            .product(product)
                            .size(vr.getSize())
                            .stockQuantity(vr.getStockQuantity() != null ? vr.getStockQuantity() : 0)
                            .sku(sku)
                            .build();
                    currentVariants.add(newVariant);
                }
            }
        }

        productRepository.save(product);

        return getProductById(product.getId());
    }

    /**
     * Cập nhật nhanh số lượng tồn kho (Admin)
     */
    @Transactional
    public void updateStock(String productId, Long variantId, Integer quantity) {
        ProductVariantEntity variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Biến thể", "id", variantId));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new BadRequestException("Biến thể không thuộc sản phẩm này");
        }

        variant.setStockQuantity(quantity);
        productVariantRepository.save(variant);
    }

    /**
     * Xóa sản phẩm (Admin) - soft delete
     */
    @Transactional
    public void deleteProduct(String id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", id));
        product.setIsActive(false);
        productRepository.save(product);
    }

    // Chuyển Entity sang Response DTO
    private ProductResponse toProductResponse(ProductEntity product) {
        List<ProductResponse.ImageDto> images = product.getImages().stream()
                .map(img -> ProductResponse.ImageDto.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .sortOrder(img.getSortOrder())
                        .isPrimary(img.getIsPrimary())
                        .build())
                .collect(Collectors.toList());

        List<ProductResponse.VariantDto> variants = product.getVariants().stream()
                .map(v -> ProductResponse.VariantDto.builder()
                        .id(v.getId())
                        .size(v.getSize())
                        .stockQuantity(v.getStockQuantity())
                        .sku(v.getSku())
                        .canDelete(!orderItemRepository.existsByVariantId(v.getId()))
                        .build())
                .collect(Collectors.toList());

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
                .originalPrice(product.getOriginalPrice())
                .salePrice(product.getSalePrice())
                .isActive(product.getIsActive())
                .isFeatured(product.getIsFeatured())
                .totalSold(product.getTotalSold())
                .avgRating(product.getAvgRating())
                .reviewCount(product.getReviewCount())
                .createdAt(product.getCreatedAt())
                .images(images)
                .variants(variants)
                .build();
    }

    // Tạo slug từ tên sản phẩm
    private String generateSlug(String name) {
        String slug = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("đ", "d").replaceAll("Đ", "D")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();

        // Đảm bảo slug unique
        if (productRepository.existsBySlug(slug)) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 6);
        }

        return slug;
    }

    // Tạo SKU tự động — đảm bảo unique bằng cách thêm timestamp ngắn
    private String generateSku(String productId, String size) {
        String base = "SKU-" + productId + "-" + size.toUpperCase();
        // Thêm suffix nếu SKU đã tồn tại
        if (productVariantRepository.existsBySku(base)) {
            base = base + "-" + Long.toHexString(System.currentTimeMillis()).substring(4).toUpperCase();
        }
        return base;
    }
}
