package shoe.com.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shoe.com.api.dto.response.ProductResponse;
import shoe.com.api.entity.ProductEntity;
import shoe.com.api.entity.UserEntity;
import shoe.com.api.entity.WishlistItemEntity;
import shoe.com.api.exception.ResourceNotFoundException;
import shoe.com.api.repository.ProductRepository;
import shoe.com.api.repository.WishlistItemRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    public List<ProductResponse> getWishlist(Long userId) {
        return wishlistItemRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(WishlistItemEntity::getProduct)
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .map(productService::toProductResponse)
                .collect(Collectors.toList());
    }

    public List<String> getProductIds(Long userId) {
        return wishlistItemRepository.findProductIdsByUserId(userId);
    }

    @Transactional
    public void add(UserEntity user, String productId) {
        if (wishlistItemRepository.findByUserIdAndProductId(user.getId(), productId).isPresent()) {
            return; // đã có, thao tác idempotent
        }
        ProductEntity product = productRepository.findById(productId)
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", productId));
        wishlistItemRepository.save(WishlistItemEntity.builder().user(user).product(product).build());
    }

    @Transactional
    public void remove(Long userId, String productId) {
        wishlistItemRepository.findByUserIdAndProductId(userId, productId)
                .ifPresent(wishlistItemRepository::delete);
    }
}
