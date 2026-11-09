package shoe.com.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shoe.com.api.service.NotificationService;
import shoe.com.api.entity.ChatMessageEntity;
import shoe.com.api.entity.UserEntity;
import shoe.com.api.exception.ResourceNotFoundException;
import shoe.com.api.repository.ChatMessageRepository;
import shoe.com.api.repository.UserRepository;

import java.util.List;

import shoe.com.api.dto.response.ChatUserResponse;
import java.util.stream.Collectors;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatService {

        private final ChatMessageRepository chatMessageRepository;
        private final UserRepository userRepository;
        private final NotificationService notificationService;

        public List<ChatUserResponse> getChatUsers(Long adminId) {
                UserEntity admin = userRepository.findById(adminId)
                                .orElseThrow(() -> new ResourceNotFoundException("Admin", "id", adminId));

                List<ChatMessageEntity> allMessages = chatMessageRepository
                                .findAllByRole(shoe.com.api.enums.Role.CUSTOMER);

                Map<Long, UserEntity> uniqueUsers = new LinkedHashMap<>();
                Map<Long, String> lastMessages = new HashMap<>();
                Map<Long, java.time.LocalDateTime> lastTimes = new HashMap<>();
                Map<Long, Long> unreadCounts = new HashMap<>();

                for (ChatMessageEntity msg : allMessages) {
                        // Xác định xem ai là khách hàng trong cuộc hội thoại này
                        UserEntity customer = msg.getSender().getRole() == shoe.com.api.enums.Role.CUSTOMER
                                        ? msg.getSender()
                                        : msg.getReceiver();

                        if (customer.getRole() != shoe.com.api.enums.Role.CUSTOMER)
                                continue; // Bỏ qua nếu không có ai là khách hàng (lỗi dữ liệu)

                        Long customerId = customer.getId();

                        if (!uniqueUsers.containsKey(customerId)) {
                                uniqueUsers.put(customerId, customer);
                                lastMessages.put(customerId, msg.getContent());
                                lastTimes.put(customerId, msg.getCreatedAt());
                        }

                        // Đếm tin nhắn chưa đọc gửi tới Admin
                        if (!msg.getIsRead() && msg.getReceiver().getRole() == shoe.com.api.enums.Role.ADMIN) {
                                unreadCounts.put(customerId, unreadCounts.getOrDefault(customerId, 0L) + 1);
                        }
                }

                return uniqueUsers.values().stream()
                                .map(user -> ChatUserResponse.from(
                                                user,
                                                lastMessages.get(user.getId()),
                                                lastTimes.get(user.getId()),
                                                unreadCounts.getOrDefault(user.getId(), 0L)))
                                .collect(Collectors.toList());
        }

        @Transactional
        public ChatMessageEntity sendMessage(Long senderId, Long receiverId, String content) {
                UserEntity sender = userRepository.findById(senderId)
                                .orElseThrow(() -> new ResourceNotFoundException("Người gửi", "id", senderId));

                UserEntity receiver;
                try {
                        receiver = userRepository.findById(receiverId)
                                        .orElseThrow(() -> new ResourceNotFoundException("Người nhận", "id",
                                                        receiverId));
                } catch (ResourceNotFoundException e) {
                        // Nếu khách hàng gửi tin nhắn nhưng không tìm thấy Admin cụ thể, gửi cho bất kỳ
                        // Admin nào
                        if (sender.getRole() == shoe.com.api.enums.Role.CUSTOMER) {
                                // Hộp thư chung: tin vẫn cần 1 receiver (cột NOT NULL) nên gán cho admin có id
                                // nhỏ nhất; trạng thái đã đọc được tính theo cuộc hội thoại, không theo admin này.
                                receiver = userRepository.findAll().stream()
                                                .filter(u -> u.getRole() == shoe.com.api.enums.Role.ADMIN)
                                                .min(java.util.Comparator.comparing(UserEntity::getId))
                                                .orElseThrow(() -> new ResourceNotFoundException("Hệ thống", "Admin",
                                                                "không tồn tại"));
                        } else {
                                throw e;
                        }
                }

                ChatMessageEntity message = ChatMessageEntity.builder()
                                .sender(sender)
                                .receiver(receiver)
                                .content(content)
                                .isRead(false)
                                .build();

                ChatMessageEntity savedMessage = chatMessageRepository.save(message);
                notificationService.sendChatMessage(savedMessage);
                return savedMessage;
        }

        public List<ChatMessageEntity> getMessagesBetween(Long user1Id, Long user2Id) {
                UserEntity user1 = userRepository.findById(user1Id)
                                .orElseThrow(() -> new ResourceNotFoundException("Người dùng 1", "id", user1Id));
                UserEntity user2 = userRepository.findById(user2Id)
                                .orElseThrow(() -> new ResourceNotFoundException("Người dùng 2", "id", user2Id));

                // Nếu có ít nhất một người là Admin, giả định Admin đang xem tin nhắn của khách
                // hàng
                // Lấy toàn bộ lịch sử chat của khách hàng đó (không phân biệt Admin nào trả
                // lời)
                if (user1.getRole() == shoe.com.api.enums.Role.ADMIN) {
                        return chatMessageRepository.findAllMessagesByUserId(user2Id);
                } else if (user2.getRole() == shoe.com.api.enums.Role.ADMIN) {
                        return chatMessageRepository.findAllMessagesByUserId(user1Id);
                }

                // Trường hợp khách hàng chat với nhau (nếu sau này có) hoặc logic mặc định
                List<ChatMessageEntity> sent = chatMessageRepository.findBySenderAndReceiverOrderByCreatedAtAsc(user1,
                                user2);
                List<ChatMessageEntity> received = chatMessageRepository.findBySenderAndReceiverOrderByCreatedAtAsc(
                                user2,
                                user1);

                sent.addAll(received);
                sent.sort((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()));

                return sent;
        }

        public List<ChatMessageEntity> getMessagesForUser(Long userId) {
                return chatMessageRepository.findAllMessagesByUserId(userId);
        }

        /** Admin mở cuộc hội thoại: đánh dấu đã đọc mọi tin khách này gửi, áp dụng cho tất cả admin */
        @Transactional
        public int markConversationRead(Long customerId) {
                int updated = chatMessageRepository.markReadFromSender(customerId);
                if (updated > 0) {
                        notificationService.sendConversationRead(customerId);
                }
                return updated;
        }

        @Transactional
        public void markAsRead(Long receiverId) {
                UserEntity receiver = userRepository.findById(receiverId)
                                .orElseThrow(() -> new ResourceNotFoundException("Người nhận", "id", receiverId));
                List<ChatMessageEntity> unread = chatMessageRepository.findByReceiverOrderByCreatedAtAsc(receiver);
                unread.forEach(msg -> msg.setIsRead(true));
                chatMessageRepository.saveAll(unread);
        }
}
