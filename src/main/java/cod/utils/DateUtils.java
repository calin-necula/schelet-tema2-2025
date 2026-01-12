package cod.utils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DateUtils {
    public static int daysBetween(String start, String end) {
        LocalDate d1 = LocalDate.parse(start);
        LocalDate d2 = LocalDate.parse(end);
        return (int) ChronoUnit.DAYS.between(d1, d2) + 1;
    }

    public static LocalDate parse(String date) {
        return LocalDate.parse(date);
    }
}