# 🎨 CraftVerse Blog API

블로그를 위한 백엔드 API 서버입니다.

## ✨ 주요 기능

### 📝 블로그 & 콘텐츠 관리
- 블로그 아티클 CRUD
- 프리미엄 콘텐츠 구매 시스템
- 슬라이드 이미지 조회

### 🎟️ 선착순 100명 이벤트
- **Redis 기반 원자적 연산**으로 동시성 제어
- Lua Script를 활용한 Race Condition 방지

### 💳 토스페이먼츠 결제 통합
- 결제 요청/승인 구현
- 웹훅을 통한 결제 상태 업데이트

### 🔐 인증 & 보안
- **JWT 토큰 기반 인증** (Access Token + Refresh Token)
- **Refresh Token Rotation (RTR)** 구현으로 보안 강화
- Google OAuth 2.0 소셜 로그인
- 이메일 인증 시스템
- 비밀번호 재설정 (인증 코드 발송)
- 토큰 블랙리스트 관리

### 📧 비동기 메시지 처리
- **RabbitMQ**를 활용한 이메일 전송 비동기 처리
- 회원가입 인증, 비밀번호 재설정 등 이메일 발송
- 
### 📊 로깅 & 모니터링
- **MDC (Mapped Diagnostic Context)** 기반 로깅
- 요청별 고유 Trace ID 자동 생성
- 사용자 IP, User-Agent, 응답 시간, HTTP 상태 코드 자동 수집
- JSON 포맷 로그

## 🛠️ 기술 스택

### Backend
- **Java 17** + **Spring Boot 3.0**
- **Spring Security**
- **Spring Data JPA**

### Database & Cache
- **PostgreSQL** - 메인 데이터베이스
- **Redis** - 선착순 이벤트 동시성 제어 및 캐싱

### Message Queue
- **RabbitMQ** - 비동기 이메일 전송 처리

### External APIs
- **Toss Payments API** - 결제 통합
- **Google OAuth 2.0** - 소셜 로그인

### Logging & Monitoring
- **SLF4J + Logback** - 로깅 프레임워크
- **MDC (Mapped Diagnostic Context)** - 분산 추적
