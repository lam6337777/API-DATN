package shoe.com.api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import shoe.com.api.entity.OrderEntity;
import shoe.com.api.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByOrderCode(String orderCode);

    Page<OrderEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<OrderEntity> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, OrderStatus status, Pageable pageable);

    // Admin: lọc đơn hàng theo trạng thái
    Page<OrderEntity> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM OrderEntity o WHERE " +
            "(cast(:status as string) IS NULL OR o.status = :status) AND " +
            "(cast(:search as string) IS NULL OR " +
            "LOWER(o.orderCode) LIKE :search OR " +
            "LOWER(o.shippingName) LIKE :search OR " +
            "LOWER(o.shippingPhone) LIKE :search OR " +
            "LOWER(o.user.email) LIKE :search) AND " +
            "(cast(:startDate as timestamp) IS NULL OR o.createdAt >= :startDate) AND " +
            "(cast(:endDate as timestamp) IS NULL OR o.createdAt <= :endDate) " +
            "ORDER BY o.createdAt DESC")
    Page<OrderEntity> findAllWithFilters(@Param("status") OrderStatus status,
            @Param("search") String search,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // Thống kê
    long countByStatus(OrderStatus status);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.createdAt >= :from")
    long countOrdersFrom(@Param("from") LocalDateTime from);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM OrderEntity o WHERE o.paymentStatus = :paymentStatus AND o.status NOT IN :excludedStatuses AND o.createdAt >= :from AND o.createdAt < :to")
    BigDecimal sumRevenueBetween(@Param("paymentStatus") shoe.com.api.enums.PaymentStatus paymentStatus,
            @Param("excludedStatuses") java.util.Collection<shoe.com.api.enums.OrderStatus> excludedStatuses,
            @Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM OrderEntity o WHERE o.paymentStatus = :paymentStatus AND o.status NOT IN :excludedStatuses")
    BigDecimal sumTotalRevenue(@Param("paymentStatus") shoe.com.api.enums.PaymentStatus paymentStatus,
            @Param("excludedStatuses") java.util.Collection<shoe.com.api.enums.OrderStatus> excludedStatuses);

    @Query("SELECT COALESCE(AVG(o.totalAmount), 0) FROM OrderEntity o WHERE o.paymentStatus = :paymentStatus AND o.status NOT IN :excludedStatuses")
    BigDecimal calculateAvgOrderValue(@Param("paymentStatus") shoe.com.api.enums.PaymentStatus paymentStatus,
            @Param("excludedStatuses") java.util.Collection<shoe.com.api.enums.OrderStatus> excludedStatuses);

    // Đơn hàng gần đây
    List<OrderEntity> findTop10ByOrderByCreatedAtDesc();
}
