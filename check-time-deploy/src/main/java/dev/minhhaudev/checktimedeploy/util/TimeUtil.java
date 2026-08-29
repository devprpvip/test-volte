package dev.minhhaudev.checktimedeploy.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class TimeUtil {

    private TimeUtil() {}

    public static Instant parseDeployTime(String timeStr, String timezone) {
        try {
            ZoneId zone = ZoneId.of(timezone);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            // config uses dd/MM but allow both; try yyyy-MM-dd first, fallback dd/MM/yyyy
            LocalDateTime ldt;
            try {
                // try dd/MM/yyyy HH:mm:ss
                DateTimeFormatter alt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                ldt = LocalDateTime.parse(timeStr, alt);
            } catch (DateTimeParseException e1) {
                try {
                    DateTimeFormatter alt2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    ldt = LocalDateTime.parse(timeStr, alt2);
                } catch (DateTimeParseException e2) {
                    // last try with slash and dash
                    ldt = LocalDateTime.parse(timeStr, fmt);
                }
            }
            return ldt.atZone(zone).toInstant();
        } catch (Exception e) {
            // fallback fixed deploy instant 12/11/2024 20:16:08 Asia/Ho_Chi_Minh
            LocalDateTime fallback = LocalDateTime.of(2024, 11, 12, 20, 16, 8);
            return fallback.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        }
    }

    public static String formatInstant(Instant instant, String pattern, String timezone) {
        try {
            ZoneId zone = ZoneId.of(timezone);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pattern).withZone(zone);
            return fmt.format(instant);
        } catch (Exception e) {
            return instant.toString();
        }
    }

    public static String formatDuration(Duration d) {
        long days = d.toDays();
        long hours = d.toHoursPart();
        long minutes = d.toMinutesPart();
        long seconds = d.toSecondsPart();
        if (days > 0) {
            return String.format("%d ngày %02d giờ %02d phút %02d giây", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%d giờ %02d phút %02d giây", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d phút %02d giây", minutes, seconds);
        } else {
            return String.format("%d giây", seconds);
        }
    }

    public static String formatDurationShort(Duration d) {
        long days = d.toDays();
        long hours = d.toHoursPart();
        long minutes = d.toMinutesPart();
        if (days > 0) return days + " ngày";
        if (hours > 0) return hours + " giờ";
        if (minutes > 0) return minutes + " phút";
        return d.toSeconds() + " giây";
    }

    public static Duration between(Instant from, Instant to) {
        if (from == null || to == null) return Duration.ZERO;
        if (to.isBefore(from)) return Duration.ZERO;
        return Duration.between(from, to);
    }
}
