package saccolink.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Date parsing helpers for the form fields (yyyy-MM-dd and yyyy-MM). */
public final class DateUtil {

    public static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private DateUtil() {
    }

    /** Raised when a form date field holds an unparseable value. */
    public static class InvalidDateException extends Exception {
        public InvalidDateException(String message) {
            super(message);
        }
    }

    /** Parses yyyy-MM-dd. */
    public static LocalDate parse(String text, String what) throws InvalidDateException {
        if (text == null || text.isBlank()) {
            throw new InvalidDateException(what + " is required (yyyy-MM-dd).");
        }
        try {
            return LocalDate.parse(text.trim(), ISO_DATE);
        } catch (DateTimeParseException e) {
            throw new InvalidDateException(what + " must be in yyyy-MM-dd format, e.g. 2025-01-15.");
        }
    }

    /** Parses yyyy-MM (contribution month). */
    public static YearMonth parseMonth(String text) throws InvalidDateException {
        if (text == null || text.isBlank()) {
            throw new InvalidDateException("Contribution month is required (yyyy-MM).");
        }
        try {
            return YearMonth.parse(text.trim(),
                    DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (DateTimeParseException e) {
            throw new InvalidDateException("Contribution month must be in yyyy-MM format, e.g. 2025-03.");
        }
    }
}
