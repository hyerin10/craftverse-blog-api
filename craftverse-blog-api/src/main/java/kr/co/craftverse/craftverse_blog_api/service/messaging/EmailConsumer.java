package kr.co.craftverse.craftverse_blog_api.service.messaging;

import java.util.UUID;
import kr.co.craftverse.craftverse_blog_api.model.MdcKey;
import kr.co.craftverse.craftverse_blog_api.model.dto.EmailMessageDTO;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
public class EmailConsumer {
  private final Logger logger;
  private final JavaMailSender mailSender;

  public EmailConsumer(Logger logger, JavaMailSender mailSender) {
    this.logger = logger;
    this.mailSender = mailSender;
  }

  @RabbitListener(queues = "${rabbitmq.queue.email.name}")
  public void consume(EmailMessageDTO emailMessageDTO, @Header(name = MdcKey.TRACE_ID, required = false) String traceId) {
    if (traceId != null)
      MDC.put(MdcKey.TRACE_ID, traceId);
    else
      MDC.put(MdcKey.TRACE_ID, "async-" + UUID.randomUUID().toString().substring(0, 8));

    try {
      logger.info("비동기 이메일 발송 프로세스 시작: {}", emailMessageDTO.getTo());
      sendEmail(emailMessageDTO);
    } finally {
      MDC.clear();
    }
  }

  private void sendEmail(EmailMessageDTO emailMessageDTO) {
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setTo(emailMessageDTO.getTo());
      message.setSubject(emailMessageDTO.getSubject());
      message.setText(emailMessageDTO.getContent());

      mailSender.send(message);
      logger.info("이메일이 성공적으로 전송되었습니다: {}", emailMessageDTO.getTo());
    } catch (Exception e) {
      logger.error("이메일 발송 중 오류 발생: {}", e.getMessage());
    }
  }
}