package shoe.com.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shoe.com.api.dto.response.OrderResponse;
import shoe.com.api.dto.response.ReturnRequestResponse;
import shoe.com.api.entity.ChatMessageEntity;
import shoe.com.api.entity.NotificationEntity;
import shoe.com.api.enums.Role;
import shoe.com.api.repository.NotificationRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final SseService sseService;

    @Transactional
    public void sendOrderNotification(OrderResponse order) {
        NotificationEntity notification = NotificationEntity.builder()
                .type("ORDER")
                .title("Đơn hàng mới")
                .description("Đơn hàng #" + order.getOrderCode() + " vừa được đặt.")
                .link("/orders")
                .build();
        notificationRepository.save(notification);
        messagingTemplate.convertAndSend("/topic/admin/orders", order);
    }

    @Transactional
    public void sendReturnNotification(ReturnRequestResponse returnRequest) {
        NotificationEntity notification = NotificationEntity.builder()
                .type("RETURN")
                .title("Yêu cầu hoàn hàng")
                .description("Có yêu cầu hoàn hàng mới cho đơn #" + returnRequest.getOrderCode())
                .link("/orders")
                .build();
        notificationRepository.save(notification);
        messagingTemplate.convertAndSend("/topic/admin/returns", returnRequest);
    }

    @Transactional
    public void sendChatMessage(ChatMessageEntity message) {
        // Mô hình hộp thư chung: mọi tin nhắn (khách gửi hoặc admin trả lời) đều phát lên
        // kênh chung của admin để tất cả admin đang online cùng thấy cuộc hội thoại cập nhật.
        if (message.getSender().getRole() == Role.CUSTOMER) {
            NotificationEntity notification = NotificationEntity.builder()
                    .type("CHAT")
                    .title("Tin nhắn mới")
                    .description("Bạn có tin nhắn mới từ " + message.getSender().getFullName())
                    .link("/support?userId=" + message.getSender().getId())
                    .build();
            notificationRepository.save(notification);
            messagingTemplate.convertAndSend("/topic/admin/chat", message);
            return;
        }

        // Admin trả lời: gửi tới khách (SSE + topic riêng) và đồng bộ cho các admin khác
        Long customerId = message.getReceiver().getId();
        messagingTemplate.convertAndSend("/topic/chat/" + customerId, message);
        sseService.sendChatMessage(customerId, message);
        messagingTemplate.convertAndSend("/topic/admin/chat", message);
    }

    /** Báo cho mọi admin: cuộc hội thoại với khách này đã được đọc */
    public void sendConversationRead(Long customerId) {
        messagingTemplate.convertAndSend("/topic/admin/chat-read", java.util.Map.of("customerId", customerId));
    }
}
