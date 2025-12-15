package kr.co.craftverse.craftverse_blog_api.common;

public class GlobalConstant {
  // Redis 키 prefix
  public static final String EMAIL_VERIFICATION_PREFIX = "email:verification:";
  public static final String ACCESS_TOKEN_PREFIX = "access_token:";
  public static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
  public static final String BLACKLIST_PREFIX = "blacklist:";
  public static final String PASSWORD_RESET_PREFIX = "password_reset:";

  // OAuth 관련
  public static final String GOGGLE_OAUTH_BASE_URL = "https://accounts.google.com/o/oauth2/v2/auth";
  public static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
  public static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
  public static final String STATE_PREFIX_ACTION = "action=";
  public static final String ACTION_LOGIN = "login";
  public static final String ACTION_SIGNUP = "signup";
  public static final String OAUTH_ERROR_ACCESS_DENIED = "access_denied";

  // OAuth 응답 타입 및 스코프
  public static final String OAUTH_RESPONSE_TYPE_CODE = "code";
  public static final String OAUTH_SCOPE_PROFILE_EMAIL = "profile email";
  public static final String OAUTH_ACCESS_TYPE_OFFLINE = "offline";
  public static final String OAUTH_PROMPT_CONSENT = "consent";

  // OAuth 요청 파라미터
  public static final String OAUTH_PARAM_CLIENT_ID = "client_id";
  public static final String OAUTH_PARAM_CLIENT_SECRET = "client_secret";
  public static final String OAUTH_PARAM_REDIRECT_URI = "redirect_uri";
  public static final String OAUTH_PARAM_RESPONSE_TYPE = "response_type";
  public static final String OAUTH_PARAM_SCOPE = "scope";
  public static final String OAUTH_PARAM_ACCESS_TYPE = "access_type";
  public static final String OAUTH_PARAM_PROMPT = "prompt";
  public static final String OAUTH_PARAM_STATE = "state";
  public static final String OAUTH_PARAM_CODE = "code";
  public static final String OAUTH_PARAM_GRANT_TYPE = "grant_type";
  public static final String OAUTH_PARAM_REFRESH_TOKEN = "refresh_token";

  // OAuth 그랜트 타입
  public static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
  public static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";

  // OAuth 응답 필드
  public static final String OAUTH_RESPONSE_ACCESS_TOKEN = "access_token";
  public static final String OAUTH_RESPONSE_REFRESH_TOKEN = "refresh_token";
  public static final String OAUTH_RESPONSE_EXPIRES_IN = "expires_in";

  // 사용자 정보 필드
  public static final String USER_INFO_EMAIL = "email";
  public static final String USER_INFO_SUB = "sub";
  public static final String USER_INFO_NAME = "name";
  public static final String USER_INFO_GIVEN_NAME = "given_name";
  public static final String USER_INFO_FAMILY_NAME = "family_name";
  public static final String USER_INFO_PICTURE = "picture";

  // OAuth 액션 결과
  public static final String OAUTH_RESULT_LOGIN_EXISTING = "login_existing";
  public static final String OAUTH_RESULT_SIGNUP_SUCCESS = "signup_success";
  public static final String OAUTH_RESULT_LOGIN_SUCCESS = "login_success";
  public static final String OAUTH_RESULT_SIGNUP_AUTO = "signup_auto";

  // OAuth 제공자
  public static final String OAUTH_PROVIDER_GOOGLE = "google";

  // 쿠키 관련
  public static final String COOKIE_AUTH_TOKEN = "auth_token";
  public static final String COOKIE_TEST_AUTH_TOKEN = "test_auth_token";
  public static final String COOKIE_SAME_SITE_LAX = "Lax";

  // URL 쿼리 파라미터
  public static final String QUERY_PARAM_LOGIN = "login";
  public static final String QUERY_PARAM_SUCCESS = "success";
  public static final String QUERY_PARAM_USER_ID = "user_id";
  public static final String QUERY_PARAM_ACTION = "action";
  public static final String QUERY_PARAM_TOKEN = "token";

  // 토큰 만료 시간
  public static final long REFRESH_TOKEN_EXPIRY_SECONDS = 30 * 24 * 60 * 60; // 30일

  // 로거 이름
  public static final String APPLICATION_LOGGER_NAME = "kr.co.craftverse.craftverse_blog_api";

  // HTTP 헤더
  public static final String AUTHORIZATION_HEADER = "Authorization";
  public static final String BEARER_PREFIX = "Bearer ";
  public static final int BEARER_PREFIX_LENGTH = 7;

  // CORS 설정
  public static final String CORS_MAPPING_PATTERN = "/**";
  public static final String CORS_ALLOWED_ORIGIN = "http://localhost:5173";
  public static final String[] CORS_ALLOWED_METHODS = {"GET", "POST", "PATCH", "DELETE", "OPTIONS", "PUT"};
  public static final String[] CORS_ALLOWED_HEADERS = {
      "Authorization",
      "Content-Type",
      "Accept",
      "X-Requested-With",
      "Cache-Control"
  };

  public static final String ARTICLE_VIEWED_COOKIE_PREFIX = "article_viewed_";
  public static final String COOKIE_VALUE_VIEWED = "viewed";
  public static final int COOKIE_MAX_AGE_ONE_YEAR = 365 * 24 * 60 * 60;
  public static final String COOKIE_PATH_ROOT = "/";

  // Sitemap 관련
  public static final String BASE_URL = "https://craftverse.co.kr";
  public static final String SITEMAP_XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
  public static final String SITEMAP_URLSET_OPEN = "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n";
  public static final String SITEMAP_URLSET_CLOSE = "</urlset>";

  // 언어 코드
  public static final String LANGUAGE_ALL = "all";
  public static final String LANGUAGE_KO = "ko";
  public static final String LANGUAGE_EN = "en";
  public static final String LANGUAGE_PATH_KO = "/ko";
  public static final String LANGUAGE_PATH_EN = "/en";

  // 카테고리
  public static final String[] CATEGORIES = {"overoll", "tech", "series"};

  // 사이트맵 우선순위 및 빈도
  public static final String PRIORITY_HOME = "1.0";
  public static final String PRIORITY_LANGUAGE_HOME = "0.9";
  public static final String PRIORITY_CATEGORY = "0.8";
  public static final String PRIORITY_ARTICLE = "0.7";

  public static final String CHANGEFREQ_DAILY = "daily";
  public static final String CHANGEFREQ_WEEKLY = "weekly";
  public static final String CHANGEFREQ_MONTHLY = "monthly";

  // URL 패턴
  public static final String URL_CATEGORY_PATTERN = "/category/";
  public static final String URL_ARTICLE_PATTERN = "/article/";

  // 결제 상태
  public static final String PAYMENT_STATUS_COMPLETED = "completed";
  public static final String PAYMENT_STATUS_COMPLETE = "complete";
  public static final String PAYMENT_STATUS_PENDING = "pending";
  public static final String PAYMENT_STATUS_FAILED = "failed";
  public static final String PAYMENT_STATUS_SUCCESS = "success";
  public static final String PAYMENT_STATUS_PAID = "paid";

  // 정규식 패턴
  public static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
  public static final String PASSWORD_VALIDATION_MESSAGE = "Password must contain at least one digit, one lowercase, one uppercase, one special character, and no whitespace";

  // 캐시 관련
  public static final String PURCHASE_CACHE_PREFIX = "article_purchase:";
  public static final long CACHE_EXPIRE_HOURS = 24;

  // 콘텐츠 관련
  public static final double PREVIEW_CONTENT_RATIO = 0.3;
  public static final String CONTENT_TRUNCATION_SUFFIX = "...";

  // 정규식
  public static final String HTML_TAG_REGEX = "<[^>]*>";
  public static final String WHITESPACE_REGEX = "\\s+";

  // 결제 방법
  public static final String PAYMENT_METHOD_CARD = "card";

  // 비밀번호 재설정 관련
  public static final String PASSWORD_RESET_VERIFIED_PREFIX = "password_reset_verified:";
  public static final String PASSWORD_RESET_VERIFIED_VALUE = "verified";

  // 인증 코드 관련
  public static final String VERIFICATION_CODE_FORMAT = "%06d";
  public static final int VERIFICATION_CODE_MAX_VALUE = 1000000;

  // 파일 경로 관련
  public static final String ARTICLE_FILE_PATH_PREFIX_WINDOWS = "C:\\home\\datakeeper\\articles\\";
  public static final String ARTICLE_FILE_PATH_PREFIX_LINUX = "home/datakeeper/articles/";
  public static final String FILE_EXT_ZIP = ".zip";
  public static final String FILE_EXT_PNG = ".png";
}