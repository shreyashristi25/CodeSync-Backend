package com.codesync.notification.repository;

import com.codesync.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUserIdAndIsReadFalse(Long userId);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = ?1 AND n.isRead = false")
    long countUnreadByUserId(Long userId);
    
    long countByUserIdAndIsReadFalse(Long userId);
}
