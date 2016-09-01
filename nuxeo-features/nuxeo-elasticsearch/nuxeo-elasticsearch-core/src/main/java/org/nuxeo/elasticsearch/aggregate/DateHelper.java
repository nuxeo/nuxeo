/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.elasticsearch.aggregate;

import org.joda.time.DateTime;

/**
 * Helper to add duration to a date with the same format as ES Date histogram interval
 *
 * @since 8.4
 */
public final class DateHelper {

    private DateHelper() {

    }

    /**
     * Returns a new datetime plus the specified duration.
     *
     * @param origin the initial datetime
     * @param duration can be expressed with a noun: hour, day, month, quarter, year
     *                 or expression: 2d, 3h, 5w, 2M, 3y
     *                 or a number of ms: 1234
     * @throws IllegalArgumentException if the duration can not be parsed
     * @return a new datetime
     */
    public static DateTime plusDuration(DateTime origin, String duration) {
        if (duration.matches("[a-zA-Z]+")) {
            return plusDurationAsNoun(origin, duration);
        }
        if (duration.matches("[0-9]+")) {
            return origin.plusMillis(Integer.valueOf(duration));
        }
        return plusDurationAsExpression(origin, duration);
    }

    private static DateTime plusDurationAsExpression(DateTime origin, String duration) {
        int k = getFactor(duration);
        switch (duration.substring(duration.length() - 1, duration.length())) {
            case "s":
                return origin.plusSeconds(k);
            case "m":
                return origin.plusMinutes(k);
            case "h":
                return origin.plusHours(k);
            case "d":
                return origin.plusDays(k);
            case "w":
                return origin.plusWeeks(k);
            case "M":
                return origin.plusMonths(k);
            case "y":
                return origin.plusYears(k);
        }
        return invalid(duration);
    }

    private static int getFactor(String duration) {
        try {
            return Integer.valueOf(duration.substring(0, duration.length() - 1));
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            invalid(duration);
        }
        return 1;
    }

    private static DateTime plusDurationAsNoun(DateTime origin, String duration) {
        switch (duration.toLowerCase()) {
            case "second":
                return origin.plusSeconds(1);
            case "minute":
                return origin.plusMinutes(1);
            case "hour":
                return origin.plusHours(1);
            case "day":
                return origin.plusDays(1);
            case "week":
                return origin.plusWeeks(1);
            case "month":
                return origin.plusMonths(1);
            case "quarter":
                return origin.plusMonths(3);
            case "year":
                return origin.plusYears(1);
        }
        return invalid(duration);
    }

    private static DateTime invalid(String msg) {
        throw new IllegalArgumentException("Invalid duration: " + msg);
    }

}
