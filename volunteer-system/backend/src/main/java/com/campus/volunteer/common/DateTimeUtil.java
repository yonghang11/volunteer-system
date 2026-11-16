package com.campus.volunteer.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间解析工具：兼容前端可能传入的几种时间格式。
 */
public class DateTimeUtil {

    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimeUtil() {
    }

    /**
     * 解析时间，支持 yyyy-MM-dd HH:mm:ss / yyyy-MM-ddTHH:mm:ss / yyyy-MM-dd HH:mm / yyyy-MM-dd。
     */
    public static LocalDateTime parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new BizException("时间不能为空");
        }
        String s = text.trim().replace('T', ' ');
        if (s.length() == 10) {
            s = s + " 00:00:00";
        }
        if (s.length() == 16) {
            s = s + ":00";
        }
        try {
            return LocalDateTime.parse(s, FMT);
        } catch (Exception e) {
            throw new BizException("时间格式不正确，应为 yyyy-MM-dd HH:mm:ss：" + text);
        }
    }

    public static String format(LocalDateTime time) {
        return time == null ? null : FMT.format(time);
    }
}
