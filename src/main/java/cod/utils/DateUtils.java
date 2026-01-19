package cod.utils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 Utility class for date operations.
 */
public final class DateUtils {

    /**
     Private constructor to prevent instantiation of utility class.
     */
    private DateUtils() {
    }

    /**
     Calculates the number of days between two dates (inclusive).
     */
    public static int daysBetween(final String start, final String end) {
        LocalDate d1 = LocalDate.parse(start);
        LocalDate d2 = LocalDate.parse(end);
        return (int) ChronoUnit.DAYS.between(d1, d2) + 1;
    }

    /**
     Parses a date string into a LocalDate.
     */
    public static LocalDate parse(final String date) {
        return LocalDate.parse(date);
    }
}
