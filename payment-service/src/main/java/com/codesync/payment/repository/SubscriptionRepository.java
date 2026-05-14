package com.codesync.payment.repository;

import com.codesync.payment.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUserId(Long userId);
    
    @Query("SELECT s FROM Subscription s WHERE s.userId = :userId AND s.status = 'ACTIVE'")
    Optional<Subscription> findActiveSubscriptionByUserId(@Param("userId") Long userId);
    
    boolean existsByUserIdAndTierAndStatus(Long userId, com.codesync.payment.entity.SubscriptionTier tier, com.codesync.payment.entity.SubscriptionStatus status);
}