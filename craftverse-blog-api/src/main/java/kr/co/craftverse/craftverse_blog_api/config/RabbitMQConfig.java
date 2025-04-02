package kr.co.craftverse.craftverse_blog_api.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

  @Value("${rabbitmq.queue.email.name}")
  private String emailQueue;

  @Value("${rabbitmq.exchange.name}")
  private String exchange;

  @Value("${rabbitmq.routing.email.key}")
  private String emailRoutingKey;

  // 이메일 발송을 위한 큐 생성
  @Bean
  public Queue emailQueue() {
    return new Queue(emailQueue);
  }

  // 메시지 교환을 위한 Exchange 생성
  @Bean
  public TopicExchange exchange() {
    return new TopicExchange(exchange);
  }

  // 큐와 exchange를 바인딩
  @Bean
  public Binding emailBinding() {
    return BindingBuilder
        .bind(emailQueue())
        .to(exchange())
        .with(emailRoutingKey);
  }

  // JSON 메시지 변환기 설정
  @Bean
  public MessageConverter converter() {
    return new Jackson2JsonMessageConverter();
  }

  // RabbitTemplate 설정
  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(converter());
    return rabbitTemplate;
  }
}