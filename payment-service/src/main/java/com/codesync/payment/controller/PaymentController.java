package com.codesync.payment.controller;

import com.codesync.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${app.razorpay.key-id}")
    private String razorpayKeyId;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestHeader("X-User-Id") Long userId) {
        try {
            Map<String, Object> order = paymentService.createOrder(userId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestHeader("X-User-Id") Long userId,
                                           @RequestBody Map<String, String> paymentData) {
        try {
            String orderId = paymentData.get("orderId");
            String paymentId = paymentData.get("paymentId");
            String signature = paymentData.get("signature");

            Map<String, Object> result = paymentService.verifyPayment(userId, orderId, paymentId, signature);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getSubscriptionStatus(@RequestHeader("X-User-Id") Long userId) {
        try {
            Map<String, Object> status = paymentService.getSubscriptionStatus(userId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/config")
    public ResponseEntity<?> getRazorpayConfig() {
        return ResponseEntity.ok(paymentService.getRazorpayConfig());
    }

    @PostMapping("/test/activate")
    public ResponseEntity<?> activateTestSubscription(@RequestHeader("X-User-Id") Long userId) {
        try {
            paymentService.activateTestSubscription(userId);
            return ResponseEntity.ok(Map.of(
                "message", "Test subscription activated!",
                "tier", "PRO",
                "status", "ACTIVE"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}