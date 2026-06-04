package shoe.com.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import shoe.com.api.entity.ChatMessageEntity;
import shoe.com.api.entity.UserEntity;
import shoe.com.api.enums.Role;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    @Query("SELECT m FROM ChatMessageEntity m JOIN FETCH m.sender JOIN FETCH m.receiver WHERE m.sender.id = :userId OR m.receiver.id = :userId ORDER BY m.createdAt ASC")
    List<ChatMessageEntity> findAllMessagesByUserId(@Param("userId") Long userId);

    List<ChatMessageEntity> findBySenderAndReceiverOrderByCreatedAtAsc(UserEntity sender, UserEntity receiver);

    List<ChatMessageEntity> findByReceiverOrderByCreatedAtAsc(UserEntity receiver);

    @Query("SELECT m FROM ChatMessageEntity m JOIN FETCH m.sender JOIN FETCH m.receiver WHERE m.sender.role = :role OR m.receiver.role = :role ORDER BY m.createdAt DESC")
    List<ChatMessageEntity> findAllByRole(@Param("role") Role role);

    List<ChatMessageEntity> findBySenderOrReceiverOrderByCreatedAtDesc(UserEntity sender, UserEntity receiver);
}
