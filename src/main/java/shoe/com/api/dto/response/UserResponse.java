package shoe.com.api.dto.response;

import lombok.Builder;
import lombok.Data;
import shoe.com.api.enums.Role;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private Role role;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private Integer totalOrders;
    private BigDecimal totalSpent;
    private String token;
}
