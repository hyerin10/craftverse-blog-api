package kr.co.craftverse.craftverse_blog_api.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class TimeUtils {

  /**
   * UTC 밀리초를 LocalDateTime으로 변환
   */
  public static LocalDateTime toLocalDateTime(Long utcMillis) {
    if (utcMillis == null) {
      return null;
    }
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(utcMillis), ZoneOffset.UTC);
  }

  /**
   * LocalDateTime을 UTC 밀리초로 변환
   */
  public static Long toUtcMillis(LocalDateTime localDateTime) {
    if (localDateTime == null) {
      return null;
    }
    return localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
  }

  /**
   * 현재 UTC 시간을 밀리초로 반환
   */
  public static Long nowUtcMillis() {
    return System.currentTimeMillis();
  }

  /**
   * UTC 밀리초를 ISO 8601 문자열로 변환
   */
  public static String toIsoString(Long utcMillis) {
    if (utcMillis == null) {
      return null;
    }
    return Instant.ofEpochMilli(utcMillis).toString();
  }
}