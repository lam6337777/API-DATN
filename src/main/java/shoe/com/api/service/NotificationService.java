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
        // Broadcast cho Admin nếu từ CUSTOMER
        if (message.getSender().getRole() == Role.CUSTOMER) {
            NotificationEntity notification = NotificationEntity.builder()
                    .type("CHAT")
                    .title("Tin nhắn mới")
                    .description("Bạn có tin nhắn mới từ " + message.getSender().getFullName())
                    .link("/chat")
                    .build();
            notificationRepository.save(notification);
            messagingTemplate.convertAndSend("/topic/admin/chat", message);
        }

        Long receiverId = message.getReceiver().getId();
        messagingTemplate.convertAndSend("/topic/chat/" + receiverId, message);
        sseService.sendChatMessage(receiverId, message);
    }
}
