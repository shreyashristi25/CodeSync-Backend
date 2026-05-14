package com.codesync.payment.service;

import com.codesync.payment.entity.Subscription;
import com.codesync.payment.entity.SubscriptionStatus;
import com.codesync.payment.entity.SubscriptionTier;
import com.codesync.payment.exception.PaymentException;
import com.codesync.payment.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Tests")
class PaymentServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private PaymentService paymentService;

    private static final Long TEST_USER_ID = 1L;
    private static final int PRO_PRICE = 19900;
    private static final int FREE_PROJECTS_LIMIT = 5;
    private static final int FREE_EXECUTIONS_LIMIT = 50;
    private static final int FREE_COLLABORATORS_LIMIT = 3;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "rzp_test_key");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "test_secret");
        ReflectionTestUtils.setField(paymentService, "proPrice", PRO_PRICE);
        ReflectionTestUtils.setField(paymentService, "freeProjectsLimit", FREE_PROJECTS_LIMIT);
        ReflectionTestUtils.setField(paymentService, "freeExecutionsLimit", FREE_EXECUTIONS_LIMIT);
        ReflectionTestUtils.setField(paymentService, "freeCollaboratorsLimit", FREE_COLLABORATORS_LIMIT);
        ReflectionTestUtils.setField(paymentService, "testMode", true);
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create mock order in test mode")
        void createOrder_TestMode_ReturnsMockOrder() {
            // Arrange
            when(subscriptionRepository.findByUserId(TEST_USER_ID))
                    .thenReturn(Optional.empty());
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // Act
            Map<String, Object> order = paymentService.createOrder(TEST_USER_ID);

            // Assert
            assertNotNull(order);
            assertNotNull(order.get("orderId"));
            assertTrue(order.get("orderId").toString().startsWith("order_"));
            assertEquals(PRO_PRICE, order.get("amount"));
            assertEquals("INR", order.get("currency"));
            assertEquals("rzp_test_key", order.get("keyId"));
            assertEquals(true, order.get("testMode"));
            verify(subscriptionRepository, times(1)).findByUserId(TEST_USER_ID);
            verify(subscriptionRepository, times(1)).save(any(Subscription.class));
        }

        @Test
        @DisplayName("Should throw exception for invalid user ID")
        void createOrder_InvalidUserId_ThrowsException() {
            // Act & Assert
            assertThrows(PaymentException.class, () -> paymentService.createOrder(null));
            assertThrows(PaymentException.class, () -> paymentService.createOrder(0L));
            assertThrows(PaymentException.class, () -> paymentService.createOrder(-1L));
        }

        @Test
        @DisplayName("Should create subscription if not exists")
        void createOrder_NewUser_CreatesSubscription() {
            // Arrange
            when(subscriptionRepository.findByUserId(TEST_USER_ID))
                    .thenReturn(Optional.empty());
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(inv -> {
                        Subscription s = inv.getArgument(0);
                        s.setId(1L);
                        return s;
                    });

            // Act
            paymentService.createOrder(TEST_USER_ID);

            // Assert
            verify(subscriptionRepository).save(argThat(sub ->
                    sub.getUserId().equals(TEST_USER_ID) &&
                    sub.getTier() == SubscriptionTier.FREE &&
                    sub.getStatus() == SubscriptionStatus.INACTIVE
            ));
        }
    }

    @Nested
    @DisplayName("verifyPayment")
    class VerifyPaymentTests {

        @Test
        @DisplayName("Should verify test payment successfully")
        void verifyPayment_TestMode_ReturnsSuccess() {
            // Arrange
            when(subscriptionRepository.findByUserId(TEST_USER_ID))
                    .thenReturn(Optional.empty());
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // Act
            Map<String, Object> result = paymentService.verifyPayment(
                    TEST_USER_ID, "order_123", "payment_123", "signature");

            // Assert
            assertNotNull(result);
            assertEquals(true, result.get("success"));
            assertEquals("PRO", result.get("tier"));
            verify(subscriptionRepository).save(argThat(sub ->
                    sub.getTier() == SubscriptionTier.PRO &&
                    sub.getStatus() == SubscriptionStatus.ACTIVE
            ));
        }

        @Test
        @DisplayName("Should throw exception for invalid user ID")
        void verifyPayment_InvalidUserId_ThrowsException() {
            assertThrows(PaymentException.class, () ->
                    paymentService.verifyPayment(null, "order", "payment", "sig"));
        }
    }

    @Nested
    @DisplayName("getSubscriptionStatus")
    class GetSubscriptionStatusTests {

        @Test
        @DisplayName("Should return default subscription for new user")
        void getSubscriptionStatus_NewUser_ReturnsDefault() {
            // Act
            Map<String, Object> status = paymentService.getSubscriptionStatus(TEST_USER_ID);

            // Assert
            assertNotNull(status);
            assertEquals(SubscriptionTier.FREE, status.get("tier"));
            assertEquals(SubscriptionStatus.INACTIVE, status.get("status"));
            assertEquals(true, status.get("testMode"));
            assertEquals(0, status.get("projectsUsed"));
            assertEquals(FREE_PROJECTS_LIMIT, status.get("projectsLimit"));
            assertEquals(0, status.get("executionsUsed"));
            assertEquals(FREE_EXECUTIONS_LIMIT, status.get("executionsLimit"));
        }

        @Test
        @DisplayName("Should return PRO subscription details")
        void getSubscriptionStatus_ProUser_ReturnsProDetails() {
            // Arrange
            Subscription proSub = Subscription.builder()
                    .id(1L)
                    .userId(TEST_USER_ID)
                    .tier(SubscriptionTier.PRO)
                    .status(SubscriptionStatus.ACTIVE)
                    .subscriptionExpiry(Instant.now().plusSeconds(86400))
                    .projectsCreatedCount(10)
                    .executionsThisMonth(100)
                    .build();

            when(subscriptionRepository.findByUserId(TEST_USER_ID))
                    .thenReturn(Optional.of(proSub));

            // Act
            Map<String, Object> status = paymentService.getSubscriptionStatus(TEST_USER_ID);

            // Assert
            assertEquals(SubscriptionTier.PRO, status.get("tier"));
            assertEquals(SubscriptionStatus.ACTIVE, status.get("status"));
            assertNotNull(status.get("expiry"));
        }

        @Test
        @DisplayName("Should return FREE tier limits for free user")
        void getSubscriptionStatus_FreeUser_ReturnsLimits() {
            // Arrange
            Subscription freeSub = Subscription.builder()
                    .id(1L)
                    .userId(TEST_USER_ID)
                    .tier(SubscriptionTier.FREE)
                    .status(SubscriptionStatus.INACTIVE)
                    .projectsCreatedCount(3)
                    .executionsThisMonth(25)
                    .build();

            when(subscriptionRepository.findByUserId(TEST_USER_ID))
                    .thenReturn(Optional.of(freeSub));

            // Act
            Map<String, Object> status = paymentService.getSubscriptionStatus(TEST_USER_ID);

            // Assert
            assertEquals(SubscriptionTier.FREE, status.get("tier"));
            assertEquals(3, status.get("projectsUsed"));
            assertEquals(FREE_PROJECTS_LIMIT, status.get("projectsLimit"));
            assertEquals(25, status.get("executionsUsed"));
            assertEquals(FREE_EXECUTIONS_LIMIT, status.get("executionsLimit"));
            assertEquals(FREE_COLLABORATORS_LIMIT, status.get("collaboratorsLimit"));
        }

        @Test
        @DisplayName("Should throw exception for invalid user ID")
        void getSubscriptionStatus_InvalidUserId_ThrowsException() {
            assertThrows(PaymentException.class, () ->
                    paymentService.getSubscriptionStatus(null));
        }
    }

    @Nested
    @DisplayName("canCreateProject")
    class CanCreateProjectTests {

        @Test
        @DisplayName("Should allow PRO user to create project")
        void canCreateProject_ProUser_ReturnsTrue() {
            // Arrange
            Subscription proSub = Subscription.builder()
                    .tier(SubscriptionTier.PRO)
                    .status(SubscriptionStatus.ACTIVE)
                    .build();
            when(subscriptionRepository.findByUserId(TEST_USER_ID))
                    .thenReturn(Optional.of(proSub));

            // Act
            boolean canCreate = paymentService.canCreateProject(TEST_USER_ID);

            // Assert
            assertTrue(canCreate);
        }

        @Test
        @DisplayName("Should allow FREE user under limit")
        void canCreateProject_FreeUserUnderLimit_ReturnsTrue() {
            // Arrange
            Subscription freeSub = Subscription.builder()
                    .tier(SubscriptionTier.FREE)
                    .status(SubscriptionStatus.INACTIVE)
                    .projectsCreatedCount(4)
                    .executionsResetDate(Instant.now())
                    .build();
            when(subscriptionRepository.findByUserId(TEST_USER_ID))
                    .thenReturn(Optional.of(freeSub));

            // Act
            boolean canCreate = paymentService.canCreateProject(TEST_USER_ID);

            // Assert
            assertTrue(canCreate);
        }

        @Test
        @DisplayName("Should deny FREE user at limit")
        void canCreateProject_FreeUserAtLimit_ReturnsFalse() {
            // Arrange
            Subscription freeSub = Subscription.builder()
                    .tier(SubscriptionTier.FREE)
                    .status(SubscriptionStatus.INACTIVE)
                    .projectsCreatedCount(5)
                    .executionsResetDate(Instant.now())
                    .build();
            when(subscriptionRepository.findByUserId(TEST_USER_ID))
                    .thenReturn(Optional.of(freeSub));

            // Act
            boolean canCreate = paymentService.canCreateProject(TEST_USER_ID);

            // Assert
            assertFalse(canCreate);
        }

        @Test
        @DisplayName("Should increment project count for FREE user")
        void incrementProjectCount_FreeUser_IncrementsCount() {
            // Arrange
            Subscription freeSub = Subscription.builder()
                    .userId(TEST_USER_ID)
                    .tier(SubscriptionTier.FREE)
                    .projectsCreatedCount(2)
                    .executionsResetDate(Instant.now())
                    .build();
            when(subscriptionRepository.findByUserId(TEST_USER_ID))
                    .thenReturn(Optional.of(freeSub));
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // Act
            paymentService.incrementProjectCount(TEST_USER_ID);

            // Assert
            verify(subscriptionRepository).save(argThat(sub ->
                    sub.getProjectsCreatedCount() == 3
            ));
        }

        @Test
        @DisplayName("Should not increment for PRO user")
        void incrementProjectCount_ProUser_DoesNotIncrement() {
            // Arrange
            Subscription proSub = Subscription.builder()
                    .tier(SubscriptionTier.PRO)
                    .projectsCreatedCount(100)
                    .build();
            when(subscriptionRepository.findByUserId(TEST_USER_ID))
                    .thenReturn(Optional.of(proSub));

            // Act
            paymentService.incrementProjectCount(TEST_USER_ID);

            // Assert - save should not be called for PRO users
            verify(subscriptionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("canExecuteCode")
    class CanExecuteCodeTests {

        @Test
        @DisplayName("Should allow PRO user to execute code")
        void canExecuteCode_ProUser_ReturnsTrue() {
            // Arrange
            Subscription proSub = Subscription.builder()
                    .tier(SubscriptionTier.PRO)
                    .status(SubscriptionStatus.ACTIVE)
                    .build();
            when(subscriptionRepository.findByUserId(TEST_USER_ID))
                    .thenReturn(Optional.of(proSub));

            // Act
            boolean canExecute = paymentService.canExecuteCode(TEST_USER_ID);

            // Assert
            assertTrue(canExecute);
        }

        @Test
        @DisplayName("Should allow FREE user under execution limit")
        void canExecuteCode_FreeUserUnderLimit_ReturnsTrue() {
            // Arrange
            Subscription freeSub = Subscription.builder()
                    .tier(SubscriptionTier.FREE)
                    .executionsThisMonth(49)
                    .executionsResetDate(Instant.now())
                    .build();
            when(subscriptionRepository.findByUserId(TEST_USER_ID))
                    .thenReturn(Optional.of(freeSub));

            // Act
            boolean canExecute = paymentService.canExecuteCode(TEST_USER_ID);

            // Assert
            assertTrue(canExecute);
        }

        @Test
        @DisplayName("Should deny FREE user at execution limit")
        void canExecuteCode_FreeUserAtLimit_ReturnsFalse() {
            // Arrange
            Subscription freeSub = Subscription.builder()
                    .tier(SubscriptionTier.FREE)
                    .executionsThisMonth(50)
                    .executionsResetDate(Instant.now())
                    .build();
            when(subscriptionRepository.findByUserId(TEST_USER_ID))
                    .thenReturn(Optional.of(freeSub));

            // Act
            boolean canExecute = paymentService.canExecuteCode(TEST_USER_ID);

            // Assert
            assertFalse(canExecute);
        }

        @Test
        @DisplayName("Should increment execution count")
        void incrementExecutionCount_FreeUser_Increments() {
            // Arrange
            Subscription freeSub = Subscription.builder()
                    .userId(TEST_USER_ID)
                    .tier(SubscriptionTier.FREE)
                    .executionsThisMonth(10)
                    .executionsResetDate(Instant.now())
                    .build();
            when(subscriptionRepository.findByUserId(TEST_USER_ID))
                    .thenReturn(Optional.of(freeSub));
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // Act
            paymentService.incrementExecutionCount(TEST_USER_ID);

            // Assert
            verify(subscriptionRepository).save(argThat(sub ->
                    sub.getExecutionsThisMonth() == 11
            ));
        }
    }

    @Nested
    @DisplayName("canAddCollaborator")
    class CanAddCollaboratorTests {

        @Test
        @DisplayName("Should allow PRO user to add any collaborators")
        void canAddCollaborator_ProUser_ReturnsTrue() {
            // Arrange
            Subscription proSub = Subscription.builder()
                    .tier(SubscriptionTier.PRO)
                    .status(SubscriptionStatus.ACTIVE)
                    .build();
            when(subscriptionRepository.findByUserId(TEST_USER_ID))
                    .thenReturn(Optional.of(proSub));

            // Act
            boolean canAdd = paymentService.canAddCollaborator(TEST_USER_ID, 100);

            // Assert
            assertTrue(canAdd);
        }

        @Test
        @DisplayName("Should allow FREE user under collaborator limit")
        void canAddCollaborator_FreeUserUnderLimit_ReturnsTrue() {
            // Arrange
            Subscription freeSub = Subscription.builder()
                    .tier(SubscriptionTier.FREE)
                    .status(SubscriptionStatus.INACTIVE)
                    .build();
            when(subscriptionRepository.findByUserId(TEST_USER_ID))
                    .thenReturn(Optional.of(freeSub));

            // Act
            boolean canAdd = paymentService.canAddCollaborator(TEST_USER_ID, 2);

            // Assert
            assertTrue(canAdd);
        }

        @Test
        @DisplayName("Should deny FREE user at collaborator limit")
        void canAddCollaborator_FreeUserAtLimit_ReturnsFalse() {
            // Arrange
            Subscription freeSub = Subscription.builder()
                    .tier(SubscriptionTier.FREE)
                    .status(SubscriptionStatus.INACTIVE)
                    .build();
            when(subscriptionRepository.findByUserId(TEST_USER_ID))
                    .thenReturn(Optional.of(freeSub));

            // Act
            boolean canAdd = paymentService.canAddCollaborator(TEST_USER_ID, 3);

            // Assert
            assertFalse(canAdd);
        }
    }

    @Nested
    @DisplayName("getRazorpayConfig")
    class GetRazorpayConfigTests {

        @Test
        @DisplayName("Should return Razorpay configuration")
        void getRazorpayConfig_ReturnsConfig() {
            // Act
            Map<String, Object> config = paymentService.getRazorpayConfig();

            // Assert
            assertNotNull(config);
            assertEquals("rzp_test_key", config.get("keyId"));
            assertEquals(true, config.get("testMode"));
        }
    }

    @Nested
    @DisplayName("activateTestSubscription")
    class ActivateTestSubscriptionTests {

        @Test
        @DisplayName("Should activate test subscription")
        void activateTestSubscription_ActivatesSuccessfully() {
            // Arrange
            when(subscriptionRepository.findByUserId(TEST_USER_ID))
                    .thenReturn(Optional.empty());
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // Act
            Map<String, Object> result = paymentService.activateTestSubscription(TEST_USER_ID);

            // Assert
            assertNotNull(result);
            assertEquals("Test subscription activated!", result.get("message"));
            assertEquals("PRO", result.get("tier"));
            assertEquals("ACTIVE", result.get("status"));
            verify(subscriptionRepository).save(argThat(sub ->
                    sub.getTier() == SubscriptionTier.PRO &&
                    sub.getStatus() == SubscriptionStatus.ACTIVE
            ));
        }

        @Test
        @DisplayName("Should upgrade existing subscription to PRO")
        void activateTestSubscription_ExistingUser_UpgradesToPro() {
            // Arrange
            Subscription existingSub = Subscription.builder()
                    .userId(TEST_USER_ID)
                    .tier(SubscriptionTier.FREE)
                    .status(SubscriptionStatus.INACTIVE)
                    .build();
            when(subscriptionRepository.findByUserId(TEST_USER_ID))
                    .thenReturn(Optional.of(existingSub));
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // Act
            paymentService.activateTestSubscription(TEST_USER_ID);

            // Assert
            verify(subscriptionRepository).save(argThat(sub ->
                    sub.getTier() == SubscriptionTier.PRO &&
                    sub.getStatus() == SubscriptionStatus.ACTIVE &&
                    sub.getSubscriptionExpiry() != null
            ));
        }
    }

    @Nested
    @DisplayName("Monthly Reset Tests")
    class MonthlyResetTests {

        @Test
        @DisplayName("Should reset execution count on new month")
        void resetMonthlyUsage_NewMonth_ResetsCount() {
            // Arrange - last reset was last month
            Instant lastMonth = Instant.now().minusSeconds(40 * 24 * 60 * 60);
            Subscription sub = Subscription.builder()
                    .tier(SubscriptionTier.FREE)
                    .executionsThisMonth(50)
                    .executionsResetDate(lastMonth)
                    .build();
            when(subscriptionRepository.findByUserId(TEST_USER_ID))
                    .thenReturn(Optional.of(sub));
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // Act
            boolean canExecute = paymentService.canExecuteCode(TEST_USER_ID);

            // Assert - should be reset and allow execution
            assertTrue(canExecute);
            verify(subscriptionRepository).save(argThat(s ->
                    s.getExecutionsThisMonth() == 0
            ));
        }

        @Test
        @DisplayName("Should not reset if same month")
        void resetMonthlyUsage_SameMonth_DoesNotReset() {
            // Arrange
            Subscription sub = Subscription.builder()
                    .tier(SubscriptionTier.FREE)
                    .executionsThisMonth(25)
                    .executionsResetDate(Instant.now())
                    .build();
            when(subscriptionRepository.findByUserId(TEST_USER_ID))
                    .thenReturn(Optional.of(sub));

            // Act
            boolean canExecute = paymentService.canExecuteCode(TEST_USER_ID);

            // Assert - should remain at 25
            assertFalse(canExecute); // At limit (50)
            verify(subscriptionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("cancelSubscription")
    class CancelSubscriptionTests {

        @Test
        @DisplayName("Should cancel active subscription")
        void cancelSubscription_ActiveSub_Cancels() {
            // Arrange
            Subscription sub = Subscription.builder()
                    .tier(SubscriptionTier.PRO)
                    .status(SubscriptionStatus.ACTIVE)
                    .build();
            when(subscriptionRepository.findByUserId(TEST_USER_ID))
                    .thenReturn(Optional.of(sub));
            when(subscriptionRepository.save(any(Subscription.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // Act
            paymentService.cancelSubscription(TEST_USER_ID);

            // Assert
            verify(subscriptionRepository).save(argThat(s ->
                    s.getStatus() == SubscriptionStatus.CANCELLED
            ));
        }
    }
}