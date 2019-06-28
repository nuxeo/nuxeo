/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *       Florent Guillaume
 */
package org.nuxeo.common.utils;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.time.temporal.ChronoUnit.YEARS;

import java.time.Duration;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The combination of a {@link Period} and a {@link Duration}.
 * <p>
 * This allows the representation of ISO 8601 "durations", which comprise a nominal duration (Java {@link Period}, i.e.,
 * years, months, days), and an accurate duration (Java {@link Duration}, i.e., hours, minutes, seconds).
 *
 * @since 11.1
 */
public final class PeriodAndDuration implements TemporalAmount {

    /**
     * A constant for a period and duration of zero.
     */
    public static final PeriodAndDuration ZERO = new PeriodAndDuration(Period.ZERO, Duration.ZERO);

    /**
     * The set of supported units. This is the concatenation of the units supported by {@link Period} and
     * {@link Duration}.
     */
    protected static final List<TemporalUnit> UNITS = List.of(YEARS, MONTHS, DAYS, SECONDS, NANOS);

    protected static final Pattern PATTERN = Pattern.compile("([-+]?)P" //
            + "(?:([-+]?[0-9]+)Y)?" //
            + "(?:([-+]?[0-9]+)M)?" //
            + "(?:([-+]?[0-9]+)D)?" //
            + "(T" //
            + "(?:([-+]?[0-9]+)H)?" //
            + "(?:([-+]?[0-9]+)M)?" //
            + "(?:([-+]?[0-9]+)(?:[.,]([0-9]{0,9}))?S)?" //
            + ")?", //
            Pattern.CASE_INSENSITIVE);

    protected static final int SECONDS_PER_MINUTE = 60;

    protected static final int SECONDS_PER_HOUR = 60 * SECONDS_PER_MINUTE;

    public final Period period;

    public final Duration duration;

    /**
     * Constructs a {@link PeriodAndDuration} from the given period and duration.
     */
    public PeriodAndDuration(Period period, Duration duration) {
        Objects.requireNonNull(period, "period");
        Objects.requireNonNull(duration, "duration");
        this.period = period;
        this.duration = duration;
    }

    /**
     * Constructs a {@link PeriodAndDuration} from the given period.
     */
    public PeriodAndDuration(Period period) {
        this(period, Duration.ZERO);
    }

    /**
     * Constructs a {@link PeriodAndDuration} from the given duration.
     */
    public PeriodAndDuration(Duration duration) {
        this(Period.ZERO, duration);
    }

    @Override
    public List<TemporalUnit> getUnits() {
        return UNITS;
    }

    @Override
    public long get(TemporalUnit unit) {
        if (unit == YEARS || unit == MONTHS || unit == DAYS) {
            return period.get(unit);
        } else {
            return duration.get(unit);
        }
    }

    @Override
    public Temporal addTo(Temporal temporal) {
        return temporal.plus(period).plus(duration);
    }

    @Override
    public Temporal subtractFrom(Temporal temporal) {
        return temporal.minus(period).minus(duration);
    }

    /**
     * Obtains a {@code PeriodAndDuration} from a text string.
     * <p>
     * This will parse the string based on the ISO-8601 period format {@code PnYnMnDTnHnMnS}. A leading minus sign, and
     * negative values for the units, are allowed.
     *
     * @param text the text to parse, not {@code null}
     * @return the period and duration (never {@code null})
     * @throws DateTimeParseException if the text cannot be parsed to a period and duration
     * @see #toString
     */
    public static PeriodAndDuration parse(String text) {
        Objects.requireNonNull(text, "text");
        Matcher matcher = PATTERN.matcher(text);
        if (matcher.matches()) {
            // check for letter T but no time sections
            if (!"T".equals(matcher.group(5))) {
                boolean negate = "-".equals(matcher.group(1));
                String yearMatch = matcher.group(2);
                String monthMatch = matcher.group(3);
                String dayMatch = matcher.group(4);
                String hourMatch = matcher.group(6);
                String minuteMatch = matcher.group(7);
                String secondMatch = matcher.group(8);
                String fractionMatch = matcher.group(9);
                if (yearMatch != null || monthMatch != null || dayMatch != null || hourMatch != null
                        || minuteMatch != null || secondMatch != null) {
                    int years = parseInt(yearMatch, text, "years");
                    int months = parseInt(monthMatch, text, "months");
                    int days = parseInt(dayMatch, text, "days");
                    long hoursAsSecs = parseNumber(hourMatch, SECONDS_PER_HOUR, text, "hours");
                    long minsAsSecs = parseNumber(minuteMatch, SECONDS_PER_MINUTE, text, "minutes");
                    long seconds = parseNumber(secondMatch, 1, text, "seconds");
                    int nanos = parseFraction(fractionMatch, Long.signum(seconds), text);
                    try {
                        Period period = Period.of(years, months, days);
                        if (negate) {
                            period = period.negated();
                        }
                        seconds = Math.addExact(hoursAsSecs, Math.addExact(minsAsSecs, seconds));
                        Duration duration = Duration.ofSeconds(seconds, nanos);
                        if (negate) {
                            duration = duration.negated();
                        }
                        return new PeriodAndDuration(period, duration);
                    } catch (ArithmeticException e) {
                        throw new DateTimeParseException("Text cannot be parsed to a PeriodAndDuration: overflow", text,
                                0, e);
                    }
                }
            }
        }
        throw new DateTimeParseException("Text cannot be parsed to a PeriodAndDuration", text, 0);
    }

    protected static int parseInt(String string, String text, String errorText) {
        if (string == null) {
            return 0;
        }
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            throw new DateTimeParseException("Text cannot be parsed to a PeriodAndDuration: " + errorText, text, 0, e);
        }
    }

    protected static long parseNumber(String string, int multiplier, String text, String errorText) {
        if (string == null) {
            return 0;
        }
        try {
            long val = Long.parseLong(string);
            return Math.multiplyExact(val, multiplier);
        } catch (NumberFormatException | ArithmeticException e) {
            throw new DateTimeParseException("Text cannot be parsed to a PeriodAndDuration: " + errorText, text, 0, e);
        }
    }

    protected static int parseFraction(String string, int sign, String text) {
        if (string == null || string.length() == 0) {
            return 0;
        }
        try {
            string = (string + "000000000").substring(0, 9);
            return Integer.parseInt(string) * sign;
        } catch (NumberFormatException | ArithmeticException e) {
            throw new DateTimeParseException("Text cannot be parsed to a PeriodAndDuration: fraction", text, 0, e);
        }
    }

    /**
     * Outputs this period and duration as a {@code String}, such as {@code P6Y3M1DT4H12M5.636224S}.
     * <p>
     * The output will be in the ISO-8601 period format. A zero period will be represented as zero seconds, "PT0S".
     *
     * @return a string representation of this period, not {@code null}
     * @see #parse
     */
    @Override
    public String toString() {
        if (period.isZero()) {
            if (duration.getSeconds() < 0) {
                return "-" + duration.negated().toString();
            } else {
                return duration.toString();
            }
        }
        if (duration.isZero()) {
            if (period.getYears() <= 0 && period.getMonths() <= 0 && period.getDays() <= 0) {
                return "-" + period.negated().toString();
            } else {
                return period.toString();
            }
        }
        StringBuilder sb = new StringBuilder();
        int i;
        if (duration.getSeconds() <= 0 && period.getYears() <= 0 && period.getMonths() <= 0 && period.getDays() <= 0) {
            // factor out minus sign
            sb.append("-");
            sb.append(period.negated().toString());
            i = sb.length();
            sb.append(duration.negated().toString());
        } else {
            sb.append(period.toString());
            i = sb.length();
            sb.append(duration.toString());
        }
        sb.deleteCharAt(i); // remove spurious second "P" from duration
        return sb.toString();
    }

}
