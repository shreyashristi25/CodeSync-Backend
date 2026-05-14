package com.codesync.execution.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExecutionJobPublisher {
    private final RabbitTemplate rabbitTemplate;

    @Value("${codesync.execution.queue}")
    private String queueName;

    public void publishJobId(String jobId) {
        rabbitTemplate.convertAndSend("", queueName, jobId);
    }
}
