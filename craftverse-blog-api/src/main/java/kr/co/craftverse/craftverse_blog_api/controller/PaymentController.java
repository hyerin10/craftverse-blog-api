package kr.co.craftverse.craftverse_blog_api.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.co.craftverse.craftverse_blog_api.common.RestResult;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.UnauthorizedException;
import kr.co.craftverse.craftverse_blog_api.config.JwtTokenProvider;
import kr.co.craftverse.craftverse_blog_api.model.dto.payment.PaymentCancelRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.payment.PaymentConfirmRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.payment.PaymentRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.payment.PaymentResponseDTO;
import kr.co.craftverse.craftverse_blog_api.security.TossWebhookSecurityValidator;
import kr.co.craftverse.craftverse_blog_api.service.TossPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/payments")
@Validated
@RequiredArgsConstructor
public class PaymentController {

  private final TossPaymentService tossPaymentService;
  private final JwtTokenProvider jwtTokenProvider;

  // PaymentController.java에 추가할 메서드

  /**
   * 결제 요청 생성 (프론트엔드에서 결제 시작 전에 호출)
   */
  @PostMapping("/payment")
  public RestResult<Map<String, Object>> createPayment(
      @Valid @RequestBody PaymentRequestDTO requestDTO,
      HttpServletRequest request) {

    Map<String, Object> data = new LinkedHashMap<>();
    Long userId = getUserIdFromToken(request);

    try {
      PaymentResponseDTO payment = tossPaymentService.createPayment(requestDTO, userId);

      data.put("payment", payment);
      data.put("orderId", payment.getOrderId());
      data.put("amount", payment.getAmount());
      data.put("message", "결제 요청이 생성되었습니다.");

      return new RestResult<>(data);

    } catch (Exception e) {
      log.error("결제 요청 생성 실패", e);
      data.put("message", "결제 요청 생성에 실패했습니다: " + e.getMessage());
      throw new RuntimeException("결제 요청 생성에 실패했습니다: " + e.getMessage());
    }
  }

  /**
   * 토큰에서 사용자 ID 추출 (공통 메서드)
   */
  private Long getUserIdFromToken(HttpServletRequest request) {
    String token = jwtTokenProvider.resolveToken(request);
    if (token == null || !jwtTokenProvider.validateToken(token)) {
      throw new UnauthorizedException();
    }
    return jwtTokenProvider.getUserId(token);
  }

  /**
   * 결제 승인 (토스페이먼츠 콜백 처리)
   */
  @PostMapping("/confirm")
  public RestResult<Map<String, Object>> confirmPayment(
      @Valid @RequestBody PaymentConfirmRequestDTO confirmRequestDTO,
      HttpServletRequest request) {

    Map<String, Object> data = new LinkedHashMap<>();
    Long userId = getUserIdFromToken(request);

    try {
      PaymentResponseDTO payment = tossPaymentService.confirmPayment(confirmRequestDTO, userId);

      data.put("payment", payment);
      data.put("message", "결제가 완료되었습니다.");

      return new RestResult<>(data);

    } catch (Exception e) {
      log.error("결제 승인 실패", e);
      data.put("message", "결제 승인에 실패했습니다: " + e.getMessage());
      throw new RuntimeException("결제 승인에 실패했습니다: " + e.getMessage());
    }
  }

  /**
   * 결제 취소
   */
  @PostMapping("/{paymentKey}/cancel")
  public RestResult<Map<String, Object>> cancelPayment(
      @PathVariable String paymentKey,
      @Valid @RequestBody PaymentCancelRequestDTO cancelRequestDTO,
      HttpServletRequest request) {

    Map<String, Object> data = new LinkedHashMap<>();
    Long userId = getUserIdFromToken(request);

    try {
      PaymentResponseDTO payment = tossPaymentService.cancelPayment(paymentKey, cancelRequestDTO, userId);

      data.put("payment", payment);
      data.put("message", "결제가 취소되었습니다.");

      return new RestResult<>(data);

    } catch (Exception e) {
      log.error("결제 취소 실패", e);
      data.put("message", "결제 취소에 실패했습니다: " + e.getMessage());
      throw new RuntimeException("결제 취소에 실패했습니다: " + e.getMessage());
    }
  }

  /**
   * 단일 결제 조회 (paymentKey로)
   */
  @GetMapping("/{paymentKey}")
  public RestResult<Map<String, Object>> getPaymentByKey(
      @PathVariable String paymentKey,
      HttpServletRequest request) {

    Map<String, Object> data = new LinkedHashMap<>();
    Long userId = getUserIdFromToken(request);

    PaymentResponseDTO payment = tossPaymentService.getPaymentByKey(paymentKey, userId);

    data.put("payment", payment);

    return new RestResult<>(data);
  }

  /**
   * 단일 결제 조회 (orderId로)
   */
  @GetMapping("/orders/{orderId}")
  public RestResult<Map<String, Object>> getPaymentByOrderId(
      @PathVariable String orderId,
      HttpServletRequest request) {

    Map<String, Object> data = new LinkedHashMap<>();
    Long userId = getUserIdFromToken(request);

    PaymentResponseDTO payment = tossPaymentService.getPaymentByOrderId(orderId, userId);

    data.put("payment", payment);

    return new RestResult<>(data);
  }

  /**
   * 사용자의 결제 내역 조회
   */
  @GetMapping("/history")
  public RestResult<Map<String, Object>> getPaymentHistory(HttpServletRequest request) {

    Map<String, Object> data = new LinkedHashMap<>();
    Long userId = getUserIdFromToken(request);

    List<PaymentResponseDTO> payments = tossPaymentService.getPaymentHistory(userId);

    data.put("payments", payments);
    data.put("count", payments.size());

    return new RestResult<>(data);
  }

  /**
   * 간단한 웹훅 처리 (개발용)
   */
  @PostMapping("/webhook")
  public RestResult<Map<String, Object>> handleWebhook(@RequestBody Map<String, Object> webhookData) {

    Map<String, Object> data = new LinkedHashMap<>();

    try {
      String eventType = (String) webhookData.get("eventType");
      log.info("웹훅 수신 - eventType: {}", eventType);
      log.info("웹훅 전체 데이터: {}", webhookData);

      // 결제 데이터 처리
      if (webhookData.containsKey("data")) {
        Map<String, Object> paymentData = (Map<String, Object>) webhookData.get("data");

        String paymentKey = (String) paymentData.get("paymentKey");
        String status = (String) paymentData.get("status");
        String orderId = (String) paymentData.get("orderId");

        log.info("결제 정보 - paymentKey: {}, status: {}, orderId: {}",
            paymentKey, status, orderId);

        // 결제 상태 업데이트
        if (paymentKey != null && status != null) {
          tossPaymentService.updatePaymentStatus(paymentKey, status);

          // 이벤트별 후처리
          switch (status) {
            case "DONE":
              tossPaymentService.handlePaymentCompleted(paymentKey, orderId);
              log.info("결제 완료 처리됨 - paymentKey: {}", paymentKey);
              break;
            case "CANCELED":
              tossPaymentService.handlePaymentCanceled(paymentKey, orderId);
              log.info("결제 취소 처리됨 - paymentKey: {}", paymentKey);
              break;
            case "PARTIAL_CANCELED":
              log.info("부분 취소 처리됨 - paymentKey: {}", paymentKey);
              break;
            default:
              log.info("기타 상태 변경 - paymentKey: {}, status: {}", paymentKey, status);
          }
        }
      }

      data.put("message", "웹훅 처리 완료");
      data.put("eventType", eventType);
      data.put("success", true);
      return new RestResult<>(data);

    } catch (Exception e) {
      log.error("웹훅 처리 실패", e);
      data.put("message", "웹훅 처리 실패: " + e.getMessage());
      data.put("success", false);
      data.put("error", e.getMessage());
      return new RestResult<>(data);
    }
  }

  /**
   * 결제 수단별 프로모션 조회
   */
  @GetMapping("/promotions")
  public RestResult<Map<String, Object>> getPromotions() {

    Map<String, Object> data = new LinkedHashMap<>();

    try {
      Map<String, Object> promotions = tossPaymentService.getPromotions();
      data.put("promotions", promotions);

      return new RestResult<>(data);

    } catch (Exception e) {
      log.error("프로모션 조회 실패", e);
      data.put("message", "프로모션 정보를 가져올 수 없습니다.");
      return new RestResult<>(data);
    }
  }

  /**
   * 카드 프로모션 조회
   */
  @GetMapping("/promotions/card")
  public RestResult<Map<String, Object>> getCardPromotions() {

    Map<String, Object> data = new LinkedHashMap<>();

    try {
      Map<String, Object> cardPromotions = tossPaymentService.getCardPromotions();
      data.put("cardPromotions", cardPromotions);

      return new RestResult<>(data);

    } catch (Exception e) {
      log.error("카드 프로모션 조회 실패", e);
      data.put("message", "카드 프로모션 정보를 가져올 수 없습니다.");
      return new RestResult<>(data);
    }
  }
}