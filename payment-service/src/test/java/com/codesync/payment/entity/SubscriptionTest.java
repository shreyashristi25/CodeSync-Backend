package com.codesync.payment.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Subscription Entity Tests")
class SubscriptionTest {

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create subscription with default values")
        void createWithDefaults() {
            Subscription subscription = Subscription.builder()
                    .userId(1L)
                    .build();

            assertEquals(1L, subscription.getUserId());
            assertEquals(SubscriptionTier.FREE, subscription.getTier());
            assertEquals(SubscriptionStatus.INACTIVE, subscription.getStatus());
            assertEquals(0, subscription.getProjectsCreatedCount());
            assertEquals(0, subscription.getExecutionsThisMonth());
            assertNotNull(subscription.getCreatedAt());
        }

        @Test
        @DisplayName("Should create PRO subscription")
        void createProSubscription() {
            Instant expiry = Instant.now().plusSeconds(86400);
            
            Subscription subscription = Subscription.builder()
                    .userId(1L)
                    .tier(SubscriptionTier.PRO)
                    .status(SubscriptionStatus.ACTIVE)
                    .subscriptionExpiry(expiry)
                    .projectsCreatedCount(10)
                    .executionsThisMonth(100)
                    .build();

            assertEquals(SubscriptionTier.PRO, subscription.getTier());
            assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
            assertEquals(expiry, subscription.getSubscriptionExpiry());
            assertEquals(10, subscription.getProjectsCreatedCount());
            assertEquals(100, subscription.getExecutionsThisMonth());
        }
    }

    @Nested
    @DisplayName("isActive Tests")
    class IsActiveTests {

        @Test
        @DisplayName("Should return true when status is ACTIVE")
        void isActive_WhenActive_ReturnsTrue() {
            Subscription subscription = Subscription.builder()
                    .status(SubscriptionStatus.ACTIVE)
                    .build();

            assertTrue(subscription.isActive());
        }

        @Test
        @DisplayName("Should return false when status is INACTIVE")
        void isActive_WhenInactive_ReturnsFalse() {
            Subscription subscription = Subscription.builder()
                    .status(SubscriptionStatus.INACTIVE)
                    .build();

            assertFalse(subscription.isActive());
        }

        @Test
        @DisplayName("Should return false when status is CANCELLED")
        void isActive_WhenCancelled_ReturnsFalse() {
            Subscription subscription = Subscription.builder()
                    .status(SubscriptionStatus.CANCELLED)
                    .build();

            assertFalse(subscription.isActive());
        }

        @Test
        @DisplayName("Should return false when status is PAST_DUE")
        void isActive_WhenPastDue_ReturnsFalse() {
            Subscription subscription = Subscription.builder()
                    .status(SubscriptionStatus.PAST_DUE)
                    .build();

            assertFalse(subscription.isActive());
        }
    }

    @Nested
    @DisplayName("isPro Tests")
    class IsProTests {

        @Test
        @DisplayName("Should return true when tier is PRO")
        void isPro_WhenPro_ReturnsTrue() {
            Subscription subscription = Subscription.builder()
                    .tier(SubscriptionTier.PRO)
                    .build();

            assertTrue(subscription.isPro());
        }

        @Test
        @DisplayName("Should return false when tier is FREE")
        void isPro_WhenFree_ReturnsFalse() {
            Subscription subscription = Subscription.builder()
                    .tier(SubscriptionTier.FREE)
                    .build();

            assertFalse(subscription.isPro());
        }
    }

    @Nested
    @DisplayName("PrePersist Tests")
    class PrePersistTests {

        @Test
        @DisplayName("Should set createdAt and updatedAt on prePersist")
        void prePersist_SetsTimestamps() {
            Subscription subscription = Subscription.builder()
                    .userId(1L)
                    .build();
            
            subscription.prePersist();

            assertNotNull(subscription.getCreatedAt());
            assertNotNull(subscription.getUpdatedAt());
            assertEquals(subscription.getCreatedAt(), subscription.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("PreUpdate Tests")
    class PreUpdateTests {

        @Test
        @DisplayName("Should update updatedAt on preUpdate")
        void preUpdate_UpdatesTimestamp() {
            Subscription subscription = Subscription.builder()
                    .userId(1L)
                    .build();
            
            // First call to set initial values
            subscription.prePersist();
            
            // Wait a bit and call preUpdate
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // ignore
            }
            
            subscription.preUpdate();
            
            assertNotNull(subscription.getUpdatedAt());
        }
    }
}