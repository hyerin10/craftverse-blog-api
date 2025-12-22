package kr.co.craftverse.craftverse_blog_api.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class MdcLoggingFilter implements Filter {
  private final Logger logger;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;

    // 1. 요청 시작 시간 기록 (밀리초)
    long startTime = System.currentTimeMillis();

    // 2. traceId 생성
    String traceId = UUID.randomUUID().toString().substring(0, 8);
    MDC.put("traceId", traceId);

    // 3. 유저 IP 및 기본 메타 정보
    String clientIp = httpRequest.getHeader("X-Forwarded-For");
    if (clientIp == null) clientIp = request.getRemoteAddr();
    MDC.put("userIp", clientIp);
    MDC.put("userAgent", httpRequest.getHeader("User-Agent"));

    // 초기 userId는 "anonymous" 등으로 설정 (인증 전일 수 있으므로)
    MDC.put("userId", "anonymous");

    try {
      chain.doFilter(request, response);
    } finally {
      // 1. 응답 코드 수집 (성공/실패 여부 확인용)
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      MDC.put("httpStatus", String.valueOf(httpResponse.getStatus()));

      // 2. 수행 시간 계산 (ms)
      long duration = System.currentTimeMillis() - startTime;
      MDC.put("elapsedTime", String.valueOf(duration));

      // 3. 마지막 로그 찍기 (이 로그에 traceId, userId, elapsedTime 등이 다 포함되어 찍힘)
      // JSON 로그 파일에 이 정보들이 한 줄의 객체로 담기게 됩니다.
      logger.info("Request Processed: [Method: {}] [URI: {}] [httpStatus: {}] [Duration: {}ms]",
          httpRequest.getMethod(),
          httpRequest.getRequestURI(),
          httpResponse.getStatus(),
          duration);

      // 4. MDC 데이터 전체 삭제
      MDC.clear();
    }
  }
}