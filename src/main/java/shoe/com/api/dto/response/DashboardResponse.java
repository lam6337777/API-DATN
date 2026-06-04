package shoe.com.api.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private Long totalOrders;
    private Long totalUsers;
    private Long totalProducts;
    private BigDecimal totalRevenue;

    private Long ordersToday;
    private BigDecimal revenueToday;

    private Long pendingOrders;
    private Long shippingOrders;

    private Long lowStockAlerts;
    private Long outOfStock;
    private BigDecimal totalInventoryValue;

    private BigDecimal avgOrderValue;

    private Long newCustomersThisMonth;
    private Long activeCustomers;
    private BigDecimal avgLifetimeValue;

    private List<OrderResponse> recentOrders;
    private List<ChartData> revenueChart;

    @Data
    @Builder
    public static class ChartData {
        private String name;
        private BigDecimal value;
    }
}
