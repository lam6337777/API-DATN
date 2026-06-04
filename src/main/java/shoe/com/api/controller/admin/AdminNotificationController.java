package shoe.com.api.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shoe.com.api.dto.response.ApiResponse;
import shoe.com.api.entity.NotificationEntity;
import shoe.com.api.repository.NotificationRepository;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationEntity>>> getAllNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<NotificationEntity> notifications = notificationRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @PutMapping("/mark-all-read")
    @Transactional
    public ResponseEntity<ApiResponse<String>> markAllAsRead() {
        notificationRepository.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu tất cả là đã đọc", null));
    }

    @PutMapping("/{id}/read")
    @Transactional
    public ResponseEntity<ApiResponse<NotificationEntity>> toggleRead(@PathVariable Long id) {
        NotificationEntity notification = notificationRepository.findById(id).orElse(null);
        if (notification != null) {
            notification.setIsRead(!notification.getIsRead());
            notificationRepository.save(notification);
        }
        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<String>> deleteNotification(@PathVariable Long id) {
        notificationRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa thông báo", null));
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<ApiResponse<String>> clearAll() {
        notificationRepository.deleteAll();
        return ResponseEntity.ok(ApiResponse.success("Đã xóa tất cả thông báo", null));
    }
}
