package shoe.com.api.enums;

public enum PaymentStatus {
    UNPAID,
    PENDING,
    PAID,
    COMPLETED,
    FAILED,
    REFUNDED,
    // Hoàn tiền một phần (trả một số sản phẩm trong đơn) - có trong dữ liệu cũ, cần giữ để đọc được đơn
    PARTIALLY_REFUNDED
}
