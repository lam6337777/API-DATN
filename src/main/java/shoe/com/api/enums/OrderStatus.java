package shoe.com.api.enums;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPING,
    DELIVERED,
    CANCELLED,
    RETURN_REQUESTED,
    RETURNED,
    // Trả một phần sản phẩm trong đơn - có trong dữ liệu cũ, cần giữ để đọc được đơn
    PARTIALLY_RETURNED,
    EXCHANGE_REQUESTED,
    EXCHANGED,
    WARRANTY_REQUESTED,
    WARRANTY_REPAIRING,
    WARRANTY_COMPLETED
}
