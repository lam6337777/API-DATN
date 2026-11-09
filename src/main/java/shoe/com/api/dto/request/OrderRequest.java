package shoe.com.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import shoe.com.api.enums.PaymentMethod;

@Data
public class OrderRequest {

    @NotBlank(message = "Tên người nhận không được để trống")
    private String shippingName;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String shippingPhone;

    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    private String shippingAddress;

    /** Tỉnh/Thành nhận hàng, dùng để tính phí ship ở backend */
    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    private String province;

    /** Mã giảm giá (không bắt buộc) */
    private String voucherCode;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;

    private String note;
}
