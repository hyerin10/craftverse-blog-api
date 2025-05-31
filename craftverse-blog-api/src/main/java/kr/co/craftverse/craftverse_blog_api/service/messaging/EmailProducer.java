package kr.co.craftverse.craftverse_blog_api.service.messaging;

import java.util.Random;
import kr.co.craftverse.craftverse_blog_api.model.dto.EmailMessageDTO;
import kr.co.craftverse.craftverse_blog_api.service.EmailVerificationService;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailProducer {
  private final Logger logger;
  private final RabbitTemplate rabbitTemplate;
  private final EmailVerificationService verificationService;

  @Value("${rabbitmq.exchange.name}")
  private String exchange;

  @Value("${rabbitmq.routing.email.key}")
  private String emailRoutingKey;

  public EmailProducer(RabbitTemplate rabbitTemplate,
      Logger logger,
      EmailVerificationService verificationService) {
    this.rabbitTemplate = rabbitTemplate;
    this.logger = logger;
    this.verificationService = verificationService;
  }

  public void sendVerificationEmail(String email) {
    String verificationCode = generateVerificationCode();

    // 인증 코드를 Redis에 저장
    verificationService.saveVerificationCode(email, verificationCode);

    EmailMessageDTO emailMessageDTO = new EmailMessageDTO(
        email,
        "회원가입 이메일 인증",
        "회원가입을 완료하려면 다음 인증 코드를 입력하세요: " + verificationCode,
        verificationCode
    );

    rabbitTemplate.convertAndSend(exchange, emailRoutingKey, emailMessageDTO);

    logger.info("[EmailProducer] 이메일 인증 메시지가 큐에 전송되었습니다. 이메일: {}", email);
  }

  private String generateVerificationCode() {
    Random random = new Random();
    int code = 100000 + random.nextInt(900000);
    return String.valueOf(code);
  }
}