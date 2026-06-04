package shoe.com.api.dto.request;

import lombok.Data;
import shoe.com.api.enums.Role;

@Data
public class AdminUpdateUserRequest {
    private String fullName;
    private String phone;
    private Role role;
}
