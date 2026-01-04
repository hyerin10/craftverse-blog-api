package kr.co.craftverse.craftverse_blog_api.filter;

import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.USER_AGENT_HEADER;
import static kr.co.craftverse.craftverse_blog_api.common.GlobalConstant.X_FORWARDED_FOR_HEADER;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.craftverse.craftverse_blog_api.model.MdcKey;
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
    MDC.put(MdcKey.TRACE_ID, traceId);

    // 3. 유저 IP 및 기본 메타 정보
    String clientIp = httpRequest.getHeader(X_FORWARDED_FOR_HEADER);
    if (clientIp == null) clientIp = request.getRemoteAddr();
    MDC.put(MdcKey.USER_IP, clientIp);
    MDC.put(MdcKey.USER_AGENT, httpRequest.getHeader(USER_AGENT_HEADER));
    MDC.put(MdcKey.USER_ID, MdcKey.ANONYMOUS);

    try {
      chain.doFilter(request, response);
    } finally {
      // 1. 응답 코드 수집 (성공/실패 여부 확인용)
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      MDC.put(MdcKey.HTTP_STATUS, String.valueOf(httpResponse.getStatus()));

      // 2. 수행 시간 계산 (ms)
      long duration = System.currentTimeMillis() - startTime;
      MDC.put(MdcKey.ELAPSED_TIME, String.valueOf(duration));

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