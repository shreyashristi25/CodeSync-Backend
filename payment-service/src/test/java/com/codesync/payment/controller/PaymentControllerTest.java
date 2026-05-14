package com.codesync.payment.controller;

import com.codesync.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentController Tests")
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private static final Long TEST_USER_ID = 1L;

    @Nested
    @DisplayName("createOrder")
    class CreateOrderTests {

        @Test
        @DisplayName("Should return order on success")
        void createOrder_ReturnsOrder() {
            // Arrange
            Map<String, Object> mockOrder = new HashMap<>();
            mockOrder.put("orderId", "order_123");
            mockOrder.put("amount", 19900);
            mockOrder.put("currency", "INR");
            mockOrder.put("keyId", "rzp_test_key");

            when(paymentService.createOrder(TEST_USER_ID)).thenReturn(mockOrder);

            // Act
            ResponseEntity<?> response = paymentController.createOrder(TEST_USER_ID);

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertEquals("order_123", body.get("orderId"));
        }

        @Test
        @DisplayName("Should return error on exception")
        void createOrder_Exception_ReturnsError() {
            // Arrange
            when(paymentService.createOrder(anyLong()))
                    .thenThrow(new RuntimeException("Payment service error"));

            // Act
            ResponseEntity<?> response = paymentController.createOrder(TEST_USER_ID);

            // Assert
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertTrue(body.containsKey("error"));
        }
    }

    @Nested
    @DisplayName("verifyPayment")
    class VerifyPaymentTests {

        @Test
        @DisplayName("Should verify payment successfully")
        void verifyPayment_ReturnsSuccess() {
            // Arrange
            Map<String, String> paymentData = new HashMap<>();
            paymentData.put("orderId", "order_123");
            paymentData.put("paymentId", "pay_123");
            paymentData.put("signature", "sig_123");

            when(paymentService.verifyPayment(anyLong(), any(), any(), any()))
                    .thenReturn(Map.of("success", true, "tier", "PRO"));

            // Act
            ResponseEntity<?> response = paymentController.verifyPayment(
                    TEST_USER_ID, paymentData);

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertEquals(true, body.get("success"));
            assertEquals("PRO", body.get("tier"));
        }

        @Test
        @DisplayName("Should return error when verification fails")
        void verifyPayment_VerificationFailed_ReturnsError() {
            // Arrange
            Map<String, String> paymentData = new HashMap<>();
            paymentData.put("orderId", "order_123");
            paymentData.put("paymentId", "pay_123");
            paymentData.put("signature", "sig_123");

            when(paymentService.verifyPayment(anyLong(), any(), any(), any()))
                    .thenThrow(new RuntimeException("Verification failed"));

            // Act
            ResponseEntity<?> response = paymentController.verifyPayment(
                    TEST_USER_ID, paymentData);

            // Assert
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("getSubscriptionStatus")
    class GetSubscriptionStatusTests {

        @Test
        @DisplayName("Should return subscription status")
        void getSubscriptionStatus_ReturnsStatus() {
            // Arrange
            Map<String, Object> mockStatus = new HashMap<>();
            mockStatus.put("tier", "FREE");
            mockStatus.put("status", "INACTIVE");
            mockStatus.put("testMode", true);
            mockStatus.put("projectsUsed", 0);
            mockStatus.put("projectsLimit", 5);

            when(paymentService.getSubscriptionStatus(TEST_USER_ID)).thenReturn(mockStatus);

            // Act
            ResponseEntity<?> response = paymentController.getSubscriptionStatus(TEST_USER_ID);

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertEquals("FREE", body.get("tier"));
            assertEquals("INACTIVE", body.get("status"));
        }

        @Test
        @DisplayName("Should return PRO status")
        void getSubscriptionStatus_ProUser_ReturnsProStatus() {
            // Arrange
            Map<String, Object> mockStatus = new HashMap<>();
            mockStatus.put("tier", "PRO");
            mockStatus.put("status", "ACTIVE");

            when(paymentService.getSubscriptionStatus(TEST_USER_ID)).thenReturn(mockStatus);

            // Act
            ResponseEntity<?> response = paymentController.getSubscriptionStatus(TEST_USER_ID);

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertEquals("PRO", body.get("tier"));
        }
    }

    @Nested
    @DisplayName("getRazorpayConfig")
    class GetRazorpayConfigTests {

        @Test
        @DisplayName("Should return Razorpay config")
        void getRazorpayConfig_ReturnsConfig() {
            // Arrange
            Map<String, Object> mockConfig = new HashMap<>();
            mockConfig.put("keyId", "rzp_test_key");
            mockConfig.put("testMode", true);

            when(paymentService.getRazorpayConfig()).thenReturn(mockConfig);

            // Act
            ResponseEntity<?> response = paymentController.getRazorpayConfig();

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertEquals("rzp_test_key", body.get("keyId"));
            assertEquals(true, body.get("testMode"));
        }
    }

    @Nested
    @DisplayName("activateTestSubscription")
    class ActivateTestSubscriptionTests {

        @Test
        @DisplayName("Should activate test subscription")
        void activateTestSubscription_ActivatesSuccessfully() {
            // Arrange
            when(paymentService.activateTestSubscription(TEST_USER_ID))
                    .thenReturn(Map.of(
                            "message", "Test subscription activated!",
                            "tier", "PRO",
                            "status", "ACTIVE"
                    ));

            // Act
            ResponseEntity<?> response = paymentController.activateTestSubscription(TEST_USER_ID);

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertEquals("Test subscription activated!", body.get("message"));
            assertEquals("PRO", body.get("tier"));
        }

        @Test
        @DisplayName("Should return error on exception")
        void activateTestSubscription_Exception_ReturnsError() {
            // Arrange
            when(paymentService.activateTestSubscription(anyLong()))
                    .thenThrow(new RuntimeException("Activation failed"));

            // Act
            ResponseEntity<?> response = paymentController.activateTestSubscription(TEST_USER_ID);

            // Assert
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }
}