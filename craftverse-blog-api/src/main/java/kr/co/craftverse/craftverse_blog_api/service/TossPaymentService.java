package kr.co.craftverse.craftverse_blog_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import kr.co.craftverse.craftverse_blog_api.exception.InvalidPaymentStatusException;
import kr.co.craftverse.craftverse_blog_api.exception.payment.PaymentAmountMismatchException;
import kr.co.craftverse.craftverse_blog_api.exception.payment.PaymentNotFoundException;
import kr.co.craftverse.craftverse_blog_api.exception.payment.PaymentProcessException;
import kr.co.craftverse.craftverse_blog_api.exception.payment.TossPaymentException;
import kr.co.craftverse.craftverse_blog_api.model.dto.*;
import kr.co.craftverse.craftverse_blog_api.model.dto.payment.PaymentCancelRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.payment.PaymentConfirmRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.payment.PaymentRequestDTO;
import kr.co.craftverse.craftverse_blog_api.model.dto.payment.PaymentResponseDTO;
import kr.co.craftverse.craftverse_blog_api.model.entity.Payment;
import kr.co.craftverse.craftverse_blog_api.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TossPaymentService {

  private final PaymentRepository paymentRepository;
  private final RestTemplate restTemplate = new RestTemplate();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Value("${toss.payments.secret-key}")
  private String secretKey;

  @Value("${toss.payments.api-url:https://api.tosspayments.com/v1}")
  private String apiUrl;

  /**
   * 결제 완료 후처리
   */
  @Transactional
  public void handlePaymentCompleted(String paymentKey, String orderId) {
    try {
      log.info("결제 완료 후처리 시작 - paymentKey: {}, orderId: {}", paymentKey, orderId);

      // 1. 결제 정보 조회
      Payment payment = paymentRepository.findByPaymentKey(paymentKey)
          .orElseThrow(() -> new RuntimeException("결제 정보를 찾을 수 없습니다: " + paymentKey));

      // 2. 주문 상태 변경 (주문 관리 시스템이 있는 경우)
      // orderService.updateOrderStatus(orderId, "PAID");

      // 3. 포인트 적립, 쿠폰 발급 등 추가 비즈니스 로직
      // pointService.earnPoints(payment.getUserId(), payment.getAmount());

      // 4. 결제 완료 알림 발송
      // notificationService.sendPaymentCompletedNotification(payment.getUserId(), payment);

      log.info("결제 완료 후처리 완료 - paymentKey: {}", paymentKey);

    } catch (Exception e) {
      log.error("결제 완료 후처리 실패 - paymentKey: {}", paymentKey, e);
      // 후처리 실패해도 결제는 완료된 상태이므로 예외를 던지지 않음
    }
  }

  /**
   * 결제 취소 후처리
   */
  @Transactional
  public void handlePaymentCanceled(String paymentKey, String orderId) {
    try {
      log.info("결제 취소 후처리 시작 - paymentKey: {}, orderId: {}", paymentKey, orderId);

      // 1. 결제 정보 조회
      Payment payment = paymentRepository.findByPaymentKey(paymentKey)
          .orElseThrow(() -> new RuntimeException("결제 정보를 찾을 수 없습니다: " + paymentKey));

      // 2. 주문 상태 변경
      // orderService.updateOrderStatus(orderId, "CANCELED");

      // 3. 포인트 차감, 쿠폰 복원 등
      // pointService.refundPoints(payment.getUserId(), payment.getAmount());

      // 4. 결제 취소 알림 발송
      // notificationService.sendPaymentCanceledNotification(payment.getUserId(), payment);

      log.info("결제 취소 후처리 완료 - paymentKey: {}", paymentKey);

    } catch (Exception e) {
      log.error("결제 취소 후처리 실패 - paymentKey: {}", paymentKey, e);
    }
  }

  /**
   * 결제 요청 생성 (카드 결제)
   */
  public PaymentResponseDTO createPayment(PaymentRequestDTO requestDTO, Long userId) {
    String orderId = generateOrderId();

    Payment payment = Payment.createPayment(
        orderId,
        requestDTO.getAmount(),
        requestDTO.getOrderName(),
        requestDTO.getCustomerEmail(),
        requestDTO.getCustomerName(),
        userId
    );

    Payment savedPayment = paymentRepository.save(payment);
    return PaymentResponseDTO.from(savedPayment);
  }

  /**
   * 결제 승인 처리 (개선된 버전)
   */
  @Transactional
  public PaymentResponseDTO confirmPayment(PaymentConfirmRequestDTO confirmDTO, Long userId) throws Exception {
    log.info("결제 승인 시작 - orderId: {}, userId: {}, amount: {}",
        confirmDTO.getOrderId(), userId, confirmDTO.getAmount());

    // DB에서 결제 정보 조회
    Payment payment = paymentRepository.findByOrderIdAndUserId(confirmDTO.getOrderId(), userId)
        .orElseThrow(() -> {
          log.error("결제 정보 없음 - orderId: {}, userId: {}", confirmDTO.getOrderId(), userId);
          return new PaymentNotFoundException("결제 정보를 찾을 수 없습니다.");
        });

    // 결제 상태 검증
    if (payment.getStatus() != Payment.PaymentStatus.READY) {
      log.error("결제 상태 오류 - orderId: {}, currentStatus: {}", confirmDTO.getOrderId(), payment.getStatus());
      throw new InvalidPaymentStatusException("결제할 수 없는 상태입니다: " + payment.getStatus());
    }

    // 금액 검증
    if (!payment.getAmount().equals(confirmDTO.getAmount())) {
      log.error("결제 금액 불일치 - orderId: {}, expected: {}, actual: {}",
          confirmDTO.getOrderId(), payment.getAmount(), confirmDTO.getAmount());
      throw new PaymentAmountMismatchException("결제 금액이 일치하지 않습니다.");
    }

    // 토스페이먼츠 API 호출
    try {
      String tossResponse = callTossConfirmAPI(confirmDTO);
      JsonNode jsonNode = objectMapper.readTree(tossResponse);

      // 결제 정보 업데이트
      payment.setPaymentKey(confirmDTO.getPaymentKey());
      payment.approve();

      Payment savedPayment = paymentRepository.save(payment);

      log.info("결제 승인 완료 - orderId: {}, paymentKey: {}",
          confirmDTO.getOrderId(), confirmDTO.getPaymentKey());

      return PaymentResponseDTO.from(savedPayment);

    } catch (HttpClientErrorException e) {
      // 토스 API 에러 처리
      handleTossAPIError(payment, e);
      throw new TossPaymentException("토스페이먼츠 승인 실패: " + e.getResponseBodyAsString());

    } catch (Exception e) {
      // 기타 예외 처리
      payment.abort();
      paymentRepository.save(payment);
      log.error("결제 승인 실패 - orderId: {}", confirmDTO.getOrderId(), e);
      throw new PaymentProcessException("결제 승인 실패: " + e.getMessage());
    }
  }

  private String callTossConfirmAPI(PaymentConfirmRequestDTO confirmDTO) {
    String url = apiUrl + "/payments/confirm";
    HttpHeaders headers = createHeaders();

    Map<String, Object> params = new HashMap<>();
    params.put("paymentKey", confirmDTO.getPaymentKey());
    params.put("orderId", confirmDTO.getOrderId());
    params.put("amount", confirmDTO.getAmount());

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, headers);

    log.debug("토스 API 호출 - URL: {}, orderId: {}", url, confirmDTO.getOrderId());

    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    return response.getBody();
  }

  private void handleTossAPIError(Payment payment, HttpClientErrorException e) {
    payment.abort();
    paymentRepository.save(payment);

    log.error("토스 API 에러 - orderId: {}, status: {}, response: {}",
        payment.getOrderId(), e.getStatusCode(), e.getResponseBodyAsString());
  }

  /**
   * 결제 취소 처리
   */
  public PaymentResponseDTO cancelPayment(String paymentKey, PaymentCancelRequestDTO cancelDTO, Long userId) throws Exception {
    Payment payment = paymentRepository.findByPaymentKey(paymentKey)
        .orElseThrow(() -> new RuntimeException("결제 정보를 찾을 수 없습니다."));

    // 사용자 권한 확인
    if (!payment.getUserId().equals(userId)) {
      throw new RuntimeException("결제 취소 권한이 없습니다.");
    }

    String url = apiUrl + "/payments/" + paymentKey + "/cancel";

    HttpHeaders headers = createHeaders();

    Map<String, Object> params = new HashMap<>();
    params.put("cancelReason", cancelDTO.getCancelReason());

    if (cancelDTO.getCancelAmount() != null) {
      params.put("cancelAmount", cancelDTO.getCancelAmount());
    }

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, headers);

    try {
      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

      // 전액 취소인 경우 CANCELED, 부분 취소인 경우 PARTIAL_CANCELED
      if (cancelDTO.getCancelAmount() == null || cancelDTO.getCancelAmount().equals(payment.getAmount())) {
        payment.cancel();
      } else {
        payment.partialCancel();
      }

      Payment savedPayment = paymentRepository.save(payment);
      return PaymentResponseDTO.from(savedPayment);

    } catch (Exception e) {
      log.error("결제 취소 실패", e);
      throw new RuntimeException("결제 취소 실패: " + e.getMessage());
    }
  }

  /**
   * paymentKey로 결제 조회
   */
  public PaymentResponseDTO getPaymentByKey(String paymentKey, Long userId) {
    // 먼저 토스페이먼츠 API에서 최신 정보 조회
    try {
      String url = apiUrl + "/payments/" + paymentKey;

      HttpHeaders headers = createHeaders();
      HttpEntity<?> request = new HttpEntity<>(headers);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
      JsonNode jsonNode = objectMapper.readTree(response.getBody());

      // DB 정보 업데이트
      Payment payment = paymentRepository.findByPaymentKey(paymentKey)
          .orElseThrow(() -> new RuntimeException("결제 정보를 찾을 수 없습니다."));

      // 사용자 권한 확인
      if (!payment.getUserId().equals(userId)) {
        throw new RuntimeException("결제 조회 권한이 없습니다.");
      }

      // 상태 동기화
      String status = jsonNode.get("status").asText();
      Payment.PaymentStatus paymentStatus = Payment.PaymentStatus.valueOf(status);
      payment.updateStatus(paymentStatus);

      if (jsonNode.has("approvedAt") && !jsonNode.get("approvedAt").isNull()) {
        // 이미 updateStatus에서 승인시간이 설정되므로 별도 처리 불필요
      }

      Payment savedPayment = paymentRepository.save(payment);
      return PaymentResponseDTO.from(savedPayment);

    } catch (Exception e) {
      log.error("결제 조회 실패", e);
      // API 호출 실패 시 DB에서만 조회
      Payment payment = paymentRepository.findByPaymentKey(paymentKey)
          .orElseThrow(() -> new RuntimeException("결제 정보를 찾을 수 없습니다."));

      if (!payment.getUserId().equals(userId)) {
        throw new RuntimeException("결제 조회 권한이 없습니다.");
      }

      return PaymentResponseDTO.from(payment);
    }
  }

  /**
   * orderId로 결제 조회
   */
  public PaymentResponseDTO getPaymentByOrderId(String orderId, Long userId) {
    try {
      String url = apiUrl + "/payments/orders/" + orderId;

      HttpHeaders headers = createHeaders();
      HttpEntity<?> request = new HttpEntity<>(headers);

      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
      JsonNode jsonNode = objectMapper.readTree(response.getBody());

      // DB 정보 업데이트
      Payment payment = paymentRepository.findByOrderIdAndUserId(orderId, userId)
          .orElseThrow(() -> new RuntimeException("결제 정보를 찾을 수 없습니다."));

      // 상태 동기화
      String status = jsonNode.get("status").asText();
      Payment.PaymentStatus paymentStatus = Payment.PaymentStatus.valueOf(status);
      payment.updateStatus(paymentStatus);

      if (jsonNode.has("paymentKey") && !jsonNode.get("paymentKey").isNull()) {
        payment.setPaymentKey(jsonNode.get("paymentKey").asText());
      }

      if (jsonNode.has("approvedAt") && !jsonNode.get("approvedAt").isNull()) {
        // 이미 updateStatus에서 승인시간이 설정되므로 별도 처리 불필요
      }

      Payment savedPayment = paymentRepository.save(payment);
      return PaymentResponseDTO.from(savedPayment);

    } catch (Exception e) {
      log.error("결제 조회 실패", e);
      // API 호출 실패 시 DB에서만 조회
      Payment payment = paymentRepository.findByOrderIdAndUserId(orderId, userId)
          .orElseThrow(() -> new RuntimeException("결제 정보를 찾을 수 없습니다."));

      return PaymentResponseDTO.from(payment);
    }
  }

  /**
   * 사용자의 결제 내역 조회
   */
  public List<PaymentResponseDTO> getPaymentHistory(Long userId) {
    List<Payment> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    return payments.stream()
        .map(PaymentResponseDTO::from)
        .toList();
  }

  /**
   * 웹훅을 통한 결제 상태 업데이트
   */
  public void updatePaymentStatus(String paymentKey, String status) {
    try {
      Payment payment = paymentRepository.findByPaymentKey(paymentKey)
          .orElseThrow(() -> new RuntimeException("결제 정보를 찾을 수 없습니다."));

      Payment.PaymentStatus paymentStatus = Payment.PaymentStatus.valueOf(status);
      payment.updateStatus(paymentStatus);

      paymentRepository.save(payment);
      log.info("결제 상태 업데이트 완료: paymentKey={}, status={}", paymentKey, status);

    } catch (Exception e) {
      log.error("결제 상태 업데이트 실패: paymentKey={}, status={}", paymentKey, status, e);
    }
  }

  /**
   * 전체 프로모션 조회
   */
  public Map<String, Object> getPromotions() throws Exception {
    String url = apiUrl + "/promotions";

    HttpHeaders headers = createHeaders();
    HttpEntity<?> request = new HttpEntity<>(headers);

    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

    return objectMapper.readValue(response.getBody(), Map.class);
  }

  /**
   * 카드 프로모션 조회
   */
  public Map<String, Object> getCardPromotions() throws Exception {
    String url = apiUrl + "/promotions/card";

    HttpHeaders headers = createHeaders();
    HttpEntity<?> request = new HttpEntity<>(headers);

    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

    return objectMapper.readValue(response.getBody(), Map.class);
  }

  /**
   * HTTP 헤더 생성 (인증 정보 포함)
   */
  private HttpHeaders createHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    String auth = Base64.getEncoder()
        .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    headers.set("Authorization", "Basic " + auth);

    return headers;
  }

  /**
   * 주문번호 생성
   */
  private String generateOrderId() {
    return "ORDER_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
  }
}