package shoe.com.api.dto.response;

import lombok.Builder;
import lombok.Getter;
import shoe.com.api.entity.UserEntity;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatUserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Long unreadCount;

    public static ChatUserResponse from(UserEntity user, String lastMessage, LocalDateTime lastMessageTime,
            Long unreadCount) {
        return ChatUserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .lastMessage(lastMessage)
                .lastMessageTime(lastMessageTime)
                .unreadCount(unreadCount)
                .build();
    }
}
