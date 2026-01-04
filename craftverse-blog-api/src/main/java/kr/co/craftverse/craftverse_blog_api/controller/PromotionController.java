package kr.co.craftverse.craftverse_blog_api.controller;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.craftverse.craftverse_blog_api.common.RestResult;
import kr.co.craftverse.craftverse_blog_api.exception.DuplicateResourceException;
import kr.co.craftverse.craftverse_blog_api.exception.OutOfStockException;
import kr.co.craftverse.craftverse_blog_api.model.MdcKey;
import kr.co.craftverse.craftverse_blog_api.model.dto.EmailMessageDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.PromotionResultDTO;
import kr.co.craftverse.craftverse_blog_api.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/promotion")
@RequiredArgsConstructor
public class PromotionController {
  private final String DRIVE_LINK = "https://docs.google.com/presentation/d/1c90CcomUt7Iraf7gN4rd1vaL_2AnKgATJQidmoI1xjY/edit?usp=drive_link";
  private final String EBOOK_FREE_PROMOTION = "EBOOK_FREE_PROMOTION";

  private final PromotionService promotionService;
  private final RabbitTemplate rabbitTemplate;

  @Value("${rabbitmq.exchange.name}")
  private String exchange;

  @Value("${rabbitmq.routing.email.key}")
  private String emailRoutingKey;

  @GetMapping("/ebook")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public RestResult<String> promotion(
      @RequestParam String email,
      HttpServletRequest request
  ) {
    MDC.put(MdcKey.EVENT_TAG, EBOOK_FREE_PROMOTION);

    String traceId = MDC.get(MdcKey.TRACE_ID);
    String userIp = MDC.get(MdcKey.USER_IP);
    if (userIp == null) userIp = request.getRemoteAddr();
    log.info("[PROMOTION_START] 상태: REQUESTED | 이메일: {} | IP: {}", email, userIp);

    PromotionResultDTO promotionResultDTO = promotionService.tryParticipatePromotion(userIp);
    MDC.put(MdcKey.REMAIN_STOCK, String.valueOf(promotionResultDTO.getRemainStock()));
    MDC.put(MdcKey.BIZ_STATUS, promotionResultDTO.getStatus());

    if (!promotionResultDTO.isSuccess()) {
      if ("DUPLICATE".equals(promotionResultDTO.getErrorCode()))
        throw new DuplicateResourceException();
      else
        throw new OutOfStockException("준비된 수량이 모두 소진되었습니다.");
    }
    log.info("[PROMOTION_WIN] 상태: {} | 남은 수량: {}", promotionResultDTO.getStatus(), promotionResultDTO.getRemainStock());

    EmailMessageDTO messageDTO = new EmailMessageDTO(
        email,
        "[당첨] 전자책 무료 나눔 이벤트 결과입니다.",
        "축하드립니다! 아래 링크에서 다운로드 받으세요.\n링크: " + DRIVE_LINK,
        null
    );

    rabbitTemplate.convertAndSend(exchange, emailRoutingKey, messageDTO, message -> {
      message.getMessageProperties().setHeader(MdcKey.TRACE_ID, traceId);
      return message;
    });

    return new RestResult<>("당첨되었습니다! 이메일로 발송된 링크를 확인해주세요.");
  }
}