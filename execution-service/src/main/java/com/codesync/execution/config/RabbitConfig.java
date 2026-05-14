package com.codesync.execution.config;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    public Queue executionQueue(@Value("${codesync.execution.queue}") String queueName) {
        return new Queue(queueName, true);
    }
}
