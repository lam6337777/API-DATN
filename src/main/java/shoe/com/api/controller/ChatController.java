package shoe.com.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import shoe.com.api.dto.response.ApiResponse;
import shoe.com.api.entity.ChatMessageEntity;
import shoe.com.api.entity.UserEntity;
import shoe.com.api.enums.Role;
import shoe.com.api.service.ChatService;
import shoe.com.api.service.SseService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import shoe.com.api.dto.response.ChatUserResponse;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SseService sseService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<ChatUserResponse>>> getChatUsers(@AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        }
        return ResponseEntity.ok(ApiResponse.success(chatService.getChatUsers(user.getId())));
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<ChatMessageEntity>> sendMessage(
            @AuthenticationPrincipal UserEntity sender,
            @RequestParam Long receiverId,
            @RequestBody String content) {
        if (sender == null)
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));

        ChatMessageEntity message = chatService.sendMessage(sender.getId(), receiverId, content);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @GetMapping("/messages/{otherId}")
    public ResponseEntity<ApiResponse<List<ChatMessageEntity>>> getMessages(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long otherId) {
        if (user == null)
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));

        // Nếu người dùng là CUSTOMER, lấy toàn bộ lịch sử của chính họ
        // (Bất kể otherId là gì, vì họ chỉ nói chuyện với Support)
        if (user.getRole() == Role.CUSTOMER) {
            return ResponseEntity.ok(ApiResponse.success(chatService.getMessagesForUser(user.getId())));
        }

        List<ChatMessageEntity> messages = chatService.getMessagesBetween(user.getId(), otherId);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    /** Khách đánh dấu đã đọc tin nhân viên gửi cho mình */
    @PostMapping("/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(@AuthenticationPrincipal UserEntity user) {
        if (user == null)
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        // Admin dùng /read/{customerId}: đánh dấu theo từng cuộc hội thoại thay vì toàn bộ hộp thư
        if (user.getRole() == Role.CUSTOMER) {
            chatService.markAsRead(user.getId());
        }
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu đã đọc"));
    }

    /** Admin mở cuộc hội thoại với khách → đã đọc cho mọi admin */
    @PostMapping("/read/{customerId}")
    public ResponseEntity<ApiResponse<Integer>> markConversationRead(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long customerId) {
        if (user == null)
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        if (user.getRole() != Role.ADMIN)
            return ResponseEntity.status(403).body(ApiResponse.error("Không có quyền"));
        return ResponseEntity.ok(ApiResponse.success(chatService.markConversationRead(customerId)));
    }

    @GetMapping(value = "/sse", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSse(@AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            throw new org.springframework.security.authentication.BadCredentialsException("Chưa đăng nhập");
        }
        return sseService.createEmitter(user.getId());
    }
}
