package kr.co.craftverse.craftverse_blog_api.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.co.craftverse.craftverse_blog_api.common.RestResult;
import kr.co.craftverse.craftverse_blog_api.common.exception.http.UnauthorizedException;
import kr.co.craftverse.craftverse_blog_api.config.JwtTokenProvider;
import kr.co.craftverse.craftverse_blog_api.model.dto.PaymentCancelRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.PaymentConfirmRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.PaymentRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.PaymentResponseDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.VirtualAccountRequestDTO;
import kr.co.craftverse.craftverse_blog_api.service.TossPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/payments")
@Validated
@RequiredArgsConstructor
public class PaymentController {

  private final TossPaymentService tossPaymentService;
  private final JwtTokenProvider jwtTokenProvider;

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
   * 일반 결제 요청 생성 (카드 결제용)
   */
  @PostMapping("/request")
  public RestResult<Map<String, Object>> createPayment(
      @Valid @RequestBody PaymentRequestDTO paymentRequestDTO,
      HttpServletRequest request) {

    Map<String, Object> data = new LinkedHashMap<>();
    Long userId = getUserIdFromToken(request);

    PaymentResponseDTO payment = tossPaymentService.createPayment(paymentRequestDTO, userId);

    data.put("payment", payment);
    data.put("message", "결제 요청이 생성되었습니다.");

    return new RestResult<>(data);
  }

  /**
   * 가상계좌 발급 요청
   */
  @PostMapping("/virtual-account")
  public RestResult<Map<String, Object>> createVirtualAccount(
      @Valid @RequestBody VirtualAccountRequestDTO virtualAccountRequestDTO,
      HttpServletRequest request) {

    Map<String, Object> data = new LinkedHashMap<>();
    Long userId = getUserIdFromToken(request);

    PaymentResponseDTO payment = tossPaymentService.createVirtualAccount(virtualAccountRequestDTO, userId);

    data.put("payment", payment);
    data.put("message", "가상계좌가 발급되었습니다.");

    return new RestResult<>(data);
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
   * 결제 상태 확인 (웹훅 처리용 - 인증 없음)
   */
  @PostMapping("/webhook")
  public RestResult<Map<String, Object>> handleWebhook(@RequestBody Map<String, Object> webhookData) {

    Map<String, Object> data = new LinkedHashMap<>();

    try {
      // 웹훅 데이터 처리
      String eventType = (String) webhookData.get("eventType");
      log.info("웹훅 수신: {}", eventType);

      // Payment 상태 업데이트 로직
      if ("Payment".equals(webhookData.get("data"))) {
        Map<String, Object> paymentData = (Map<String, Object>) webhookData.get("data");
        String paymentKey = (String) paymentData.get("paymentKey");
        String status = (String) paymentData.get("status");

        tossPaymentService.updatePaymentStatus(paymentKey, status);
      }

      data.put("message", "웹훅 처리 완료");
      return new RestResult<>(data);

    } catch (Exception e) {
      log.error("웹훅 처리 실패", e);
      data.put("message", "웹훅 처리 실패");
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