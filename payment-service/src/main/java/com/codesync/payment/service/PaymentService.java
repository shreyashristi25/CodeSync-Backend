package com.codesync.payment.service;

import com.codesync.payment.entity.Subscription;
import com.codesync.payment.entity.SubscriptionStatus;
import com.codesync.payment.entity.SubscriptionTier;
import com.codesync.payment.exception.PaymentException;
import com.codesync.payment.repository.SubscriptionRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class PaymentService {

    private final SubscriptionRepository subscriptionRepository;
    private RazorpayClient razorpayClient;

    @Value("${app.razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${app.razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${app.subscription.pro-price}")
    private int proPrice;

    @Value("${app.subscription.free-projects-limit:5}")
    private int freeProjectsLimit;

    @Value("${app.subscription.free-executions-limit:50}")
    private int freeExecutionsLimit;

    @Value("${app.subscription.free-collaborators-limit:3}")
    private int freeCollaboratorsLimit;

    @Value("${app.razorpay.test-mode:true}")
    private boolean testMode;

    public PaymentService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @PostConstruct
    public void init() throws RazorpayException {
        this.razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
    }

    public Map<String, Object> createOrder(Long userId) {
        validateUserId(userId);
        getOrCreateSubscription(userId);

        return createRazorpayOrder();
    }

    private Map<String, Object> createRazorpayOrder() {
        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", proPrice);
            orderRequest.put("currency", "INR");
            orderRequest.put("payment_capture", 1);

            com.razorpay.Order order = razorpayClient.Orders.create(orderRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.get("id"));
            response.put("amount", order.get("amount"));
            response.put("currency", order.get("currency"));
            response.put("keyId", razorpayKeyId);
            response.put("testMode", testMode);
            return response;
        } catch (RazorpayException e) {
            throw new PaymentException("Failed to create payment order: " + e.getMessage());
        }
    }

    public Map<String, Object> verifyPayment(Long userId, String orderId, String paymentId, String signature) {
        validateUserId(userId);

        activateSubscription(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("tier", "PRO");
        result.put("message", "Payment successful!");
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSubscriptionStatus(Long userId) {
        validateUserId(userId);

        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSubscription(userId));

        resetMonthlyUsageIfNeeded(subscription);

        Map<String, Object> status = new HashMap<>();
        status.put("tier", subscription.getTier());
        status.put("status", subscription.getStatus());
        status.put("expiry", subscription.getSubscriptionExpiry());
        status.put("testMode", testMode);

        if (subscription.getTier() == SubscriptionTier.FREE) {
            status.put("projectsUsed", subscription.getProjectsCreatedCount());
            status.put("projectsLimit", freeProjectsLimit);
            status.put("executionsUsed", subscription.getExecutionsThisMonth());
            status.put("executionsLimit", freeExecutionsLimit);
            status.put("collaboratorsLimit", freeCollaboratorsLimit);
        }

        return status;
    }

    @Transactional(readOnly = true)
    public boolean canCreateProject(Long userId) {
        Subscription subscription = getOrCreateSubscription(userId);
        resetMonthlyUsageIfNeeded(subscription);
        
        if (subscription.isPro()) {
            return true;
        }
        
        return subscription.getProjectsCreatedCount() < freeProjectsLimit;
    }

    public void incrementProjectCount(Long userId) {
        Subscription subscription = getOrCreateSubscription(userId);
        resetMonthlyUsageIfNeeded(subscription);
        
        if (!subscription.isPro()) {
            subscription.setProjectsCreatedCount(subscription.getProjectsCreatedCount() + 1);
            subscriptionRepository.save(subscription);
        }
    }

    @Transactional(readOnly = true)
    public boolean canExecuteCode(Long userId) {
        Subscription subscription = getOrCreateSubscription(userId);
        resetMonthlyUsageIfNeeded(subscription);
        
        if (subscription.isPro()) {
            return true;
        }
        
        return subscription.getExecutionsThisMonth() < freeExecutionsLimit;
    }

    public void incrementExecutionCount(Long userId) {
        Subscription subscription = getOrCreateSubscription(userId);
        resetMonthlyUsageIfNeeded(subscription);
        
        if (!subscription.isPro()) {
            subscription.setExecutionsThisMonth(subscription.getExecutionsThisMonth() + 1);
            subscriptionRepository.save(subscription);
        }
    }

    @Transactional(readOnly = true)
    public boolean canAddCollaborator(Long userId, int currentCollaborators) {
        Subscription subscription = getOrCreateSubscription(userId);
        
        if (subscription.isPro()) {
            return true;
        }
        
        return currentCollaborators < freeCollaboratorsLimit;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRazorpayConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("keyId", razorpayKeyId);
        config.put("testMode", testMode);
        return config;
    }

    public Map<String, Object> activateTestSubscription(Long userId) {
        validateUserId(userId);
        
        activateSubscription(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Test subscription activated!");
        result.put("tier", "PRO");
        result.put("status", "ACTIVE");
        return result;
    }

    public void cancelSubscription(Long userId) {
        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new PaymentException("Subscription not found for user: " + userId));
        
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new PaymentException("Invalid user ID");
        }
    }

    private Subscription getOrCreateSubscription(Long userId) {
        return subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSubscription(userId));
    }

    private Subscription createDefaultSubscription(Long userId) {
        Subscription subscription = Subscription.builder()
                .userId(userId)
                .tier(SubscriptionTier.FREE)
                .status(SubscriptionStatus.INACTIVE)
                .projectsCreatedCount(0)
                .executionsThisMonth(0)
                .build();
        
        return subscriptionRepository.save(subscription);
    }

    private void activateSubscription(Long userId) {
        Subscription subscription = getOrCreateSubscription(userId);
        
        subscription.setTier(SubscriptionTier.PRO);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setSubscriptionExpiry(Instant.now().plusSeconds(30L * 24 * 60 * 60));
        
        subscriptionRepository.save(subscription);
    }

    private void resetMonthlyUsageIfNeeded(Subscription subscription) {
        LocalDate now = LocalDate.now();
        Instant resetDate = subscription.getExecutionsResetDate();
        
        if (resetDate == null) {
            subscription.setExecutionsResetDate(Instant.now());
            subscriptionRepository.save(subscription);
            return;
        }
        
        int resetMonth = resetDate.atZone(ZoneId.systemDefault()).toLocalDate().getMonthValue();
        if (resetMonth != now.getMonthValue()) {
            subscription.setExecutionsThisMonth(0);
            subscription.setExecutionsResetDate(Instant.now());
            subscriptionRepository.save(subscription);
        }
    }
}