package shoe.com.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shoe.com.api.dto.response.DashboardResponse;
import shoe.com.api.dto.response.OrderResponse;
import shoe.com.api.enums.OrderStatus;
import shoe.com.api.repository.OrderRepository;
import shoe.com.api.repository.ProductRepository;
import shoe.com.api.repository.UserRepository;

import shoe.com.api.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

        private final OrderRepository orderRepository;
        private final UserRepository userRepository;
        private final ProductRepository productRepository;
        private final OrderService orderService;

        public DashboardResponse getDashboardStats() {
                LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

                List<OrderStatus> excludedStatuses = Arrays.asList(OrderStatus.CANCELLED, OrderStatus.RETURNED,
                                OrderStatus.RETURN_REQUESTED);

                long totalOrders = orderRepository.count();
                long totalUsers = userRepository.count();
                long totalProducts = productRepository.countByIsActiveTrue();
                BigDecimal totalRevenue = orderRepository.sumTotalRevenue(PaymentStatus.PAID, excludedStatuses);

                long ordersToday = orderRepository.countOrdersFrom(todayStart);
                BigDecimal revenueToday = orderRepository.sumRevenueBetween(PaymentStatus.PAID, excludedStatuses,
                                todayStart, LocalDateTime.now());

                long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
                long shippingOrders = orderRepository.countByStatus(OrderStatus.SHIPPING);

                // Inventory Stats
                long lowStockAlerts = productRepository.countLowStock();
                long outOfStock = productRepository.countOutOfStock();
                BigDecimal totalInventoryValue = productRepository.calculateTotalInventoryValue();

                // Order Stats
                BigDecimal avgOrderValue = orderRepository.calculateAvgOrderValue(PaymentStatus.PAID, excludedStatuses);

                // Customer Stats
                LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                                .withNano(0);
                long newCustomersThisMonth = userRepository.countNewUsersSince(monthStart);
                long activeCustomers = userRepository.countByIsActiveTrue();
                BigDecimal avgLifetimeValue = totalUsers > 0
                                ? totalRevenue.divide(BigDecimal.valueOf(totalUsers), 2, BigDecimal.ROUND_HALF_UP)
                                : BigDecimal.ZERO;

                List<OrderResponse> recentOrders = orderRepository.findTop10ByOrderByCreatedAtDesc()
                                .stream()
                                .map(orderService::toOrderResponse)
                                .collect(Collectors.toList());

                // Dữ liệu biểu đồ doanh thu 7 ngày gần nhất
                List<DashboardResponse.ChartData> revenueChart = new java.util.ArrayList<>();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

                for (int i = 6; i >= 0; i--) {
                        LocalDateTime start = todayStart.minusDays(i);
                        LocalDateTime end = start.plusDays(1);

                        BigDecimal dayRevenue = orderRepository.sumRevenueBetween(PaymentStatus.PAID, excludedStatuses,
                                        start, end);

                        revenueChart.add(DashboardResponse.ChartData.builder()
                                        .name(start.format(formatter))
                                        .value(dayRevenue)
                                        .build());
                }

                return DashboardResponse.builder()
                                .totalOrders(totalOrders)
                                .totalUsers(totalUsers)
                                .totalProducts(totalProducts)
                                .totalRevenue(totalRevenue)
                                .ordersToday(ordersToday)
                                .revenueToday(revenueToday)
                                .pendingOrders(pendingOrders)
                                .shippingOrders(shippingOrders)
                                .lowStockAlerts(lowStockAlerts)
                                .outOfStock(outOfStock)
                                .totalInventoryValue(totalInventoryValue)
                                .avgOrderValue(avgOrderValue)
                                .newCustomersThisMonth(newCustomersThisMonth)
                                .activeCustomers(activeCustomers)
                                .avgLifetimeValue(avgLifetimeValue)
                                .recentOrders(recentOrders)
                                .revenueChart(revenueChart)
                                .build();
        }
}
