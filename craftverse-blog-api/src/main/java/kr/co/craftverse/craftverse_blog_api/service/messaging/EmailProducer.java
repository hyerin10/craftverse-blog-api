package kr.co.craftverse.craftverse_blog_api.service.messaging;

import kr.co.craftverse.craftverse_blog_api.model.dto.EmailMessageDTO;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailProducer {
  private final Logger logger;
  private final RabbitTemplate rabbitTemplate;

  @Value("${rabbitmq.exchange.name}")
  private String exchange;

  @Value("${rabbitmq.routing.email.key}")
  private String emailRoutingKey;

  public EmailProducer(RabbitTemplate rabbitTemplate, Logger logger) {
    this.rabbitTemplate = rabbitTemplate;
    this.logger = logger;
  }

  /**
   * 이메일 인증 코드 발송 (코드는 외부에서 생성하여 전달)
   */
  public void sendVerificationEmail(String email, String verificationCode) {
    EmailMessageDTO emailMessageDTO = new EmailMessageDTO(
        email,
        "회원가입 이메일 인증",
        "회원가입을 완료하려면 다음 인증 코드를 입력하세요: " + verificationCode,
        verificationCode
    );

    rabbitTemplate.convertAndSend(exchange, emailRoutingKey, emailMessageDTO);
    logger.info("[EmailProducer] 이메일 인증 메시지가 큐에 전송되었습니다. 이메일: {}", email);
  }

  /**
   * 비밀번호 재설정 인증 코드 발송
   */
  public void sendPasswordResetEmail(String email, String verificationCode) {
    EmailMessageDTO emailMessageDTO = new EmailMessageDTO(
        email,
        "비밀번호 재설정 인증",
        "비밀번호를 재설정하려면 다음 인증 코드를 입력하세요: " + verificationCode,
        verificationCode
    );

    rabbitTemplate.convertAndSend(exchange, emailRoutingKey, emailMessageDTO);
    logger.info("[EmailProducer] 비밀번호 재설정 메시지가 큐에 전송되었습니다. 이메일: {}", email);
  }
}