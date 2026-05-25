package com.ssscloud.auction.common.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public final class DateTimeUtils {
  private DateTimeUtils() {} // tránh vô tình khởi tạo

  // có thể viết cái chuyển đổi format

  // Kiểm tra thời gian có nằm trong X giây cuối cùng không (Anti-sniping)
  public static boolean isInLastSeconds(LocalDateTime endTime, int seconds) {
    if (endTime == null) return false;
    long remainingSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), endTime);
    return remainingSeconds > 0 && remainingSeconds <= seconds;
  }

  // Tính thời gian còn lại đến endTime (đơn vị giây)
  public static long getRemainingSeconds(LocalDateTime endTime) {
    if (endTime == null) return 0;
    return ChronoUnit.SECONDS.between(LocalDateTime.now(), endTime);
  }

  // trả về now
  public static LocalDateTime now() {
    return LocalDateTime.now();
  }

  // Kiểm tra xem thời gian có quá hạn
  public static boolean isExpired(LocalDateTime endTime) {
    return endTime != null && LocalDateTime.now().isAfter(endTime);
  }
}
