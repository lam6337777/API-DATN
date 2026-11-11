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
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getProductReviews(String productId, int page, int size, Integer rating, boolean withComment) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        int star = (rating != null && rating >= 1 && rating <= 5) ? rating : 0;
        return reviewRepository.searchByProduct(productId, star, withComment, pageable)
                .map(r -> {
                    ReviewResponse dto = toReviewResponse(r);
                    // Trang công khai: che bớt tên khách và không lộ mã đơn
                    dto.setFullName(maskName(dto.getFullName()));
                    dto.setOrderCode(null);
                    return dto;
                });
    }

    /** Tổng quan đánh giá: điểm trung bình, tổng số, số có bình luận, phân bố 1–5 sao */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getProductReviewSummary(String productId) {
        java.util.Map<String, Long> counts = new java.util.LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) counts.put(String.valueOf(i), 0L);
        long total = 0;
        double sum = 0;
        for (Object[] row : reviewRepository.countByRating(productId)) {
            int star = ((Number) row[0]).intValue();
            long n = ((Number) row[1]).longValue();
            if (star >= 1 && star <= 5) counts.put(String.valueOf(star), n);
            total += n;
            sum += (double) star * n;
        }
        java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("average", total == 0 ? 0 : Math.round(sum / total * 10) / 10.0);
        out.put("total", total);
        out.put("withComment", reviewRepository.countWithComment(productId));
        out.put("counts", counts);
        return out;
    }

    /** "Nguyễn Văn An" → "N*********n" (giống cách các sàn TMĐT ẩn tên người đánh giá) */
    static String maskName(String name) {
        if (name == null || name.isBlank()) return "***";
        String s = name.trim();
        int len = s.codePointCount(0, s.length());
        if (len <= 2) return s.substring(0, s.offsetByCodePoints(0, 1)) + "*";
        String first = s.substring(0, s.offsetByCodePoints(0, 1));
        String last = s.substring(s.offsetByCodePoints(0, len - 1));
        return first + "*".repeat(Math.min(len - 2, 8)) + last;
    }

    /** Admin: tất cả đánh giá, mới nhất trước */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getAllReviews(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        return reviewRepository.findAll(pageable).map(this::toReviewResponse);
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

        // Phân loại (size/màu) khách đã mua trong đơn của đánh giá này
        String variantSize = null, variantColor = null;
        if (review.getOrder() != null && review.getOrder().getOrderItems() != null) {
            String pid = review.getProduct().getId();
            for (var it : review.getOrder().getOrderItems()) {
                if (it.getVariant() != null && it.getVariant().getProduct() != null
                        && pid.equals(it.getVariant().getProduct().getId())) {
                    variantSize = it.getSize();
                    variantColor = it.getColor();
                    break;
                }
            }
        }

        return ReviewResponse.builder()
                .id(review.getId())
                .variantSize(variantSize)
                .variantColor(variantColor)
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
