package shoe.com.api.enums;

public enum PaymentMethod {
    COD,
    
    // Giữ lại các phương thức cũ để tương thích với dữ liệu lịch sử trong Database
    // Không hiển thị ở Client khi đặt hàng mới
    VNPAY,
    BANK_TRANSFER,
    CASH
}
