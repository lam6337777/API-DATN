package shoe.com.api.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import shoe.com.api.entity.ChatMessageEntity;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(Long userId) {
        // Hạn thời gian kết nối là 24 giờ
        SseEmitter emitter = new SseEmitter(86400000L);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError((e) -> emitters.remove(userId));

        // Gửi sự kiện INIT thành công
        try {
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data("Connected successfully"));
        } catch (IOException e) {
            emitters.remove(userId);
        }

        return emitter;
    }

    public void sendChatMessage(Long receiverId, ChatMessageEntity message) {
        SseEmitter emitter = emitters.get(receiverId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("CHAT")
                        .data(message));
            } catch (IOException e) {
                emitters.remove(receiverId);
                emitter.completeWithError(e);
            }
        }
    }
}
