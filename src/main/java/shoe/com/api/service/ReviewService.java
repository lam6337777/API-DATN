package shoe.com.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shoe.com.api.dto.response.ReviewResponse;
import shoe.com.api.entity.ProductEntity;
import shoe.com.api.entity.ReviewEntity;
import shoe.com.api.entity.UserEntity;
import shoe.com.api.entity.OrderEntity;
import shoe.com.api.enums.OrderStatus;
import shoe.com.api.exception.BadRequestException;
import shoe.com.api.exception.ResourceNotFoundException;
import shoe.com.api.repository.OrderRepository;
import shoe.com.api.repository.ProductRepository;
import shoe.com.api.repository.ReviewRepository;
import shoe.com.api.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    /**
     * Lấy danh sách đánh giá của sản phẩm (Page DTO)
     */
    public Page<ReviewResponse> getProductReviews(String productId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(this::toReviewResponse);
    }

    public Page<ReviewResponse> getUserReviews(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toReviewResponse);
    }

    /**
     * Tạo đánh giá
     */
    @Transactional
    public ReviewResponse createReview(UserEntity user, String productId, Long orderId, Integer rating,
            String comment) {
        // Validate rating
        if (rating < 1 || rating > 5) {
            throw new BadRequestException("Rating phải từ 1 đến 5");
        }

        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", productId));

        // Kiểm tra đơn hàng đã giao chưa
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "id", orderId));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestException("Chỉ có thể đánh giá đơn hàng đã giao thành công");
        }

        // Kiểm tra đã đánh giá chưa
        if (reviewRepository.existsByUserIdAndProductIdAndOrderId(user.getId(), productId, orderId)) {
            throw new BadRequestException("Bạn đã đánh giá sản phẩm này trong đơn hàng này rồi");
        }

        ReviewEntity review = ReviewEntity.builder()
                .product(product)
                .user(user)
                .order(order)
                .rating(rating)
                .comment(comment)
                .build();

        review = reviewRepository.save(review);

        // Cập nhật avgRating và reviewCount cho product
        Float avgRating = reviewRepository.avgRatingByProductId(productId);
        long reviewCount = reviewRepository.countByProductId(productId);
        product.setAvgRating(avgRating);
        product.setReviewCount((int) reviewCount);
        productRepository.save(product);

        return toReviewResponse(review);
    }

    private ReviewResponse toReviewResponse(ReviewEntity review) {
        String productImage = null;
        if (review.getProduct().getImages() != null && !review.getProduct().getImages().isEmpty()) {
            productImage = review.getProduct().getImages().stream()
                    .filter(img -> img.getIsPrimary())
                    .findFirst()
                    .orElse(review.getProduct().getImages().get(0))
                    .getImageUrl();
        }

        return ReviewResponse.builder()
                .id(review.getId())
                .fullName(review.getUser().getFullName())
                .avatarUrl(review.getUser().getAvatarUrl())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .orderCode(review.getOrder() != null ? review.getOrder().getOrderCode() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .productName(review.getProduct().getName())
                .productImage(productImage)
                .createdAt(review.getCreatedAt())
                .build();
    }

    /**
     * Admin: Xóa đánh giá
     */
    @Transactional
    public void deleteReview(Long reviewId) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Đánh giá", "id", reviewId));

        String productId = review.getProduct().getId();
        reviewRepository.delete(review);

        // Cập nhật lại avgRating
        ProductEntity product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            Float avgRating = reviewRepository.avgRatingByProductId(productId);
            long reviewCount = reviewRepository.countByProductId(productId);
            product.setAvgRating(avgRating);
            product.setReviewCount((int) reviewCount);
            productRepository.save(product);
        }
    }
}
