package shoe.com.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shoe.com.api.dto.request.AdminUpdateUserRequest;
import shoe.com.api.dto.request.ChangePasswordRequest;
import shoe.com.api.dto.request.UpdateProfileRequest;
import shoe.com.api.dto.response.UserResponse;
import shoe.com.api.entity.UserEntity;
import shoe.com.api.enums.Role;
import shoe.com.api.exception.BadRequestException;
import shoe.com.api.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Lấy danh sách người dùng (Page DTO)
     */
    public Page<UserResponse> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserEntity> users = userRepository.findAll(pageable);
        return users.map(user -> authService.toUserResponse(user, null));
    }

    /**
     * Lấy chi tiết người dùng
     */
    public UserResponse getUserById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new shoe.com.api.exception.ResourceNotFoundException("Người dùng", "id", id));
        return authService.toUserResponse(user, null);
    }

    /**
     * Cập nhật thông tin bởi Admin
     */
    @Transactional
    public UserResponse updateUserByAdmin(Long userId, AdminUpdateUserRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new shoe.com.api.exception.ResourceNotFoundException("Người dùng", "id", userId));

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getRole() != null) user.setRole(request.getRole());

        user = userRepository.save(user);
        return authService.toUserResponse(user, null);
    }

    /**
     * Cập nhật thông tin cá nhân
     */
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new shoe.com.api.exception.ResourceNotFoundException("Người dùng", "id", userId));

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());

        user = userRepository.save(user);
        return authService.toUserResponse(user, null);
    }

    /**
     * Đổi mật khẩu
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new shoe.com.api.exception.ResourceNotFoundException("Người dùng", "id", userId));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu cũ không chính xác");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
