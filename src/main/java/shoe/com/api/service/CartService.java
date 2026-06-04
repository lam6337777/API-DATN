package shoe.com.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shoe.com.api.dto.response.CartItemResponse;
import shoe.com.api.entity.*;
import shoe.com.api.exception.BadRequestException;
import shoe.com.api.exception.ResourceNotFoundException;
import shoe.com.api.repository.CartItemRepository;
import shoe.com.api.repository.ProductVariantRepository;
import shoe.com.api.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    /**
     * Xem giỏ hàng
     */
    public List<CartItemResponse> getCartItems(Long userId) {
        List<CartItemEntity> items = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return items.stream().map(this::toCartItemResponse).collect(Collectors.toList());
    }

    /**
     * Thêm sản phẩm vào giỏ hàng
     */
    @Transactional
    public CartItemResponse addToCart(Long userId, Long variantId, Integer quantity) {
        ProductVariantEntity variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Biến thể sản phẩm", "id", variantId));

        // Kiểm tra tồn kho
        if (variant.getStockQuantity() < quantity) {
            throw new BadRequestException("Số lượng tồn kho không đủ. Còn lại: " + variant.getStockQuantity());
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", userId));

        if (!user.getIsActive()) {
            throw new BadRequestException("Tài khoản của bạn đã bị vô hiệu hóa.");
        }

        // Nếu đã có trong giỏ, cộng dồn số lượng
        Optional<CartItemEntity> existingItem = cartItemRepository.findByUserIdAndVariantId(userId, variantId);
        if (existingItem.isPresent()) {
            CartItemEntity item = existingItem.get();
            int newQty = item.getQuantity() + quantity;
            if (newQty > variant.getStockQuantity()) {
                throw new BadRequestException("Vượt quá số lượng tồn kho. Còn lại: " + variant.getStockQuantity());
            }
            item.setQuantity(newQty);
            return toCartItemResponse(cartItemRepository.save(item));
        }

        CartItemEntity cartItem = CartItemEntity.builder()
                .user(user)
                .variant(variant)
                .quantity(quantity)
                .build();

        return toCartItemResponse(cartItemRepository.save(cartItem));
    }

    /**
     * Cập nhật số lượng
     */
    @Transactional
    public CartItemResponse updateCartItem(Long userId, Long cartItemId, Integer quantity) {
        CartItemEntity item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Giỏ hàng", "id", cartItemId));

        if (!item.getUser().getId().equals(userId)) {
            throw new BadRequestException("Không có quyền cập nhật");
        }

        if (!item.getUser().getIsActive()) {
            throw new BadRequestException("Tài khoản của bạn đã bị vô hiệu hóa.");
        }

        if (quantity <= 0) {
            cartItemRepository.delete(item);
            return null;
        }

        if (quantity > item.getVariant().getStockQuantity()) {
            throw new BadRequestException("Vượt quá tồn kho. Còn lại: " + item.getVariant().getStockQuantity());
        }

        item.setQuantity(quantity);
        return toCartItemResponse(cartItemRepository.save(item));
    }

    /**
     * Xóa item khỏi giỏ
     */
    @Transactional
    public void removeCartItem(Long userId, Long cartItemId) {
        CartItemEntity item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Giỏ hàng", "id", cartItemId));

        if (!item.getUser().getId().equals(userId)) {
            throw new BadRequestException("Không có quyền xóa");
        }

        cartItemRepository.delete(item);
    }

    /**
     * Xóa toàn bộ giỏ hàng
     */
    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    // Chuyển Entity sang DTO
    private CartItemResponse toCartItemResponse(CartItemEntity item) {
        ProductVariantEntity variant = item.getVariant();
        ProductEntity product = variant.getProduct();

        // Tìm ảnh chính
        String primaryImageUrl = product.getImages().stream()
                .filter(ProductImageEntity::getIsPrimary)
                .findFirst()
                .map(ProductImageEntity::getImageUrl)
                .orElse(product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl());

        BigDecimal unitPrice = product.getSalePrice() != null ? product.getSalePrice() : product.getOriginalPrice();

        return CartItemResponse.builder()
                .id(item.getId())
                .variantId(variant.getId())
                .productName(product.getName())
                .productSlug(product.getSlug())
                .primaryImageUrl(primaryImageUrl)
                .size(variant.getSize())
                .color(variant.getColor())
                .quantity(item.getQuantity())
                .unitPrice(unitPrice)
                .totalPrice(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())))
                .stockQuantity(variant.getStockQuantity())
                .build();
    }
}
