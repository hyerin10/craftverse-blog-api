package kr.co.craftverse.craftverse_blog_api.model;

public class MdcKey {
  public static final String TRACE_ID = "traceId";
  public static final String USER_IP = "userIp";
  public static final String USER_AGENT = "userAgent";

  public static final String USER_ID = "userId";
  public static final String ANONYMOUS = "anonymous";

  public static final String HTTP_STATUS = "httpStatus";
  public static final String ELAPSED_TIME = "elapsedTime";

  public static final String EVENT_TAG = "eventTag";

  public static final String REMAIN_STOCK = "remainStock";
  public static final String BIZ_STATUS = "bizStatus";

  private MdcKey() {
    throw new AssertionError("Cannot instantiate constant class");
  }
}