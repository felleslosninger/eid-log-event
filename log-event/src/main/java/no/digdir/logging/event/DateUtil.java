package no.digdir.logging.event;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.regex.Pattern;

/**
 * Utility methods copied and adapted from https://github.com/felleslosninger/idporten-validators/blob/main/src/main/java/no/idporten/validators/identifier/PersonIdentifierValidator.java
 */
class DateUtil {

    private static final Pattern ELEVEN_DIGITS = Pattern.compile("^\\d{11}$");

    static LocalDate computeDOB(String eventSubjectPid) {
        if (!ELEVEN_DIGITS.matcher(eventSubjectPid).matches()) {
            return null;
        }

        boolean isDnumber = false;
        int[] digits = eventSubjectPid.chars().map(i -> Character.digit((char) i, 10)).toArray();
        if (digits[0] >= 4) {
            isDnumber = true;
        }
        if (digits[0] > 7) {
            return null;
        }
        final int INDIVID_NR_END_INDEX = 9;
        final int INDIVID_NR_START_INDEX = 6;

        int day = isDnumber ? (10 * digits[0] - 40 + digits[1]) : (10 * digits[0] + digits[1]);
        int month = getMonth(10 * digits[2] + digits[3]);
        int year = 10 * digits[4] + digits[5];
        final int individNumber = Integer.parseInt(eventSubjectPid.substring(INDIVID_NR_START_INDEX, INDIVID_NR_END_INDEX));
        final int century = getCentury(individNumber, year, isDnumber);
        if (century < 0) {
            return null;
        }

        return parseDate(day, month, century, year);
    }

    private static LocalDate parseDate(final int day, int month, int century, int year) {
        final String dateString = String.format("%02d%02d%02d%02d", day, month, century, year);
        try {
            final DateFormat df = new SimpleDateFormat("ddMMyyyy");
            df.setLenient(false);
            return df.parse(dateString)
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        } catch (final ParseException e) {
            return null;
        }
    }

    private static int getMonth(int month) {
        if (isSyntheticSkatt(month)) {
            return month - 80;
        } else if (isSyntheticNHN(month)) {
            return month - 65;
        } else if (isSyntheticNav(month)) {
            return month - 40;
        } else if (isSyntheticDigdir(month)) {
            return month - 20;
        } else {
            return month;
        }
    }

    private static boolean isSyntheticNav(int month) {
        return month > 40 && month <= 52;
    }

    private static boolean isSyntheticNHN(int month) {
        return month > 65 && month <= 77;
    }

    private static boolean isSyntheticSkatt(int month) {
        return month > 80 && month <= 92;
    }

    private static boolean isSyntheticDigdir(int month) {
        return month > 20 && month <= 32;
    }

    private static int getCentury(final int individnumber, final int birthYear, boolean isDnumber) {
        if (isDnumber) {
            if (individnumber >= 500 && individnumber <= 599) {
                return 18;
            } else if (individnumber <= 199 && birthYear < 40) {
                return 19;
            } else if ((individnumber <= 499 || (individnumber >= 600 && individnumber <= 999)) && birthYear >= 40) {
                return 19;
            } else if (individnumber >= 200 && individnumber <= 999 && birthYear < 40) {
                return 20;
            }
        } else { // fnr
            if (individnumber <= 499) {
                return 19;
            }
            if (individnumber >= 500 && individnumber <= 749 && birthYear > 54) {
                return 18;
            }
            if (individnumber >= 500 && birthYear < 40) {
                return 20;
            }
            if (individnumber >= 900 && birthYear >= 40) {
                return 19;
            }
        }
        return -1;
    }

}
