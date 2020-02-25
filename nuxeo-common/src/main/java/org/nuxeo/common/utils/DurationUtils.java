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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.common.utils;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @since 11.1
 */
public final class DurationUtils {

    public static final Pattern DURATION_SIMPLE_FORMAT = Pattern.compile(
            "(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?(?:(\\d+)ms)?");

    private DurationUtils() {
        // utility class
    }

    /**
     * Obtains a {@code Duration} from a text string such as {@code PnDTnHnMn.nS} or {@code _d_h_m_s_ms}.
     * <p>
     * See {@link Duration#parse(CharSequence)} for {@code PnDTnHnMn.nS} format.
     * <p>
     * For {@code _d_h_m_s_ms}, there are five sections, each consisting of a number and a suffix. The suffixes are "d",
     * "h", "m", "s" and "ms" for days, hours, minutes, seconds and milliseconds. The suffixes must occur in order and
     * at least one of them must be present.
     *
     * @throws DateTimeParseException if the text cannot be parsed to a duration
     * @see Duration#parse(CharSequence)
     */
    public static Duration parse(String value) {
        if (value.startsWith("P") || value.startsWith("-P")) {
            // Duration JDK format
            return Duration.parse(value);
        }
        Matcher matcher = DURATION_SIMPLE_FORMAT.matcher(value);
        if (matcher.matches()) {

            long days = 0;
            long hours = 0;
            long minutes = 0;
            long seconds = 0;
            long millis = 0;
            if (matcher.group(1) != null) {
                days = Long.parseLong(matcher.group(1));
            }
            if (matcher.group(2) != null) {
                hours = Long.parseLong(matcher.group(2));
            }
            if (matcher.group(3) != null) {
                minutes = Long.parseLong(matcher.group(3));
            }
            if (matcher.group(4) != null) {
                seconds = Long.parseLong(matcher.group(4));
            }
            if (matcher.group(5) != null) {
                millis = Long.parseLong(matcher.group(5));
            }
            return Duration.ofDays(days).plusHours(hours).plusMinutes(minutes).plusSeconds(seconds).plusMillis(millis);
        }
        throw new DateTimeParseException("Text cannot be parsed to a Duration", value, 0);
    }

    /**
     * Obtains a {@code Duration} from a text string according to {@link #parse(String)}, but in case of invalid, zero
     * or negative duration returns a default.
     *
     * @param value the value to parse
     * @param defaultDuration the default duration to return for invalid, zero or negative duration
     * @return the parsed duration (positive), or the default
     * @since 11.1
     */
    public static Duration parsePositive(String value, Duration defaultDuration) {
        if (isBlank(value)) {
            return defaultDuration;
        }
        try {
            Duration duration = parse(value);
            if (duration.isZero() || duration.isNegative()) {
                return defaultDuration;
            } else {
                return duration;
            }
        } catch (DateTimeParseException e) {
            return defaultDuration;
        }
    }

}
