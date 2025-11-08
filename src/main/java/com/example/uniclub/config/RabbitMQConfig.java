package com.example.uniclub.config;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ========= 🏛 Club Application =========
    public static final String CLUB_APP_EXCHANGE = "clubApplicationExchange";
    public static final String CLUB_APP_QUEUE = "clubApplicationQueue";
    public static final String CLUB_APP_ROUTING_KEY = "club.application.status";

    // ========= 🔔 Notification System =========
    public static final String NOTIFICATION_EXCHANGE = "notificationExchange";
    public static final String NOTIFICATION_QUEUE = "notificationQueue";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.event";

    @Bean
    public TopicExchange clubExchange() {
        return new TopicExchange(CLUB_APP_EXCHANGE);
    }

    @Bean
    public Queue clubQueue() {
        return new Queue(CLUB_APP_QUEUE, true);
    }

    @Bean
    public Binding clubBinding(Queue clubQueue, TopicExchange clubExchange) {
        return BindingBuilder.bind(clubQueue)
                .to(clubExchange)
                .with(CLUB_APP_ROUTING_KEY);
    }

    // 🧩 NEW: Exchange, Queue, Binding cho Notification
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(notificationExchange)
                .with(NOTIFICATION_ROUTING_KEY);
    }

    // ⚙️ Converter và template bạn đã có, giữ nguyên
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        return factory;
    }
}
