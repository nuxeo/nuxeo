/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.utils;

import java.time.DateTimeException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.ecm.platform.suggestbox.service.suggesters.DocumentSearchByDateSuggester;

/**
 * @deprecated since 11.1 only used from deprecated {@link DocumentSearchByDateSuggester}
 */
@Deprecated
public class DateMatcher {

    private static final Pattern YEAR_ONLY_MATCHER = Pattern.compile("^\\d{4}$");

    private static final Pattern MONTH_DIGIT_ONLY_MATCHER = Pattern.compile("^\\d{2}$");

    private static final Pattern YEAR_MONTHS_MATCHER = Pattern.compile("^\\d{4}[_ -:]\\d{2}$");

    private static final Pattern MONTHS_YEAR_MATCHER = Pattern.compile("^\\d{2}[_ -:]\\d{4}$");

    private static final Pattern MONTHS_DAY_YEAR_MATCHER = Pattern.compile("^\\d{2}[_ -:]\\d{2,}[_ -:]\\d{4}$");

    private static final Pattern YEAR_MONTHS_DAY_MATCHER = Pattern.compile("^\\d{4}[_ -:]\\d{2,}[_ -:]\\d{2}$");

    private boolean withYears = false;

    private boolean withMonth = false;

    private boolean withDay = false;

    private final Calendar dateSuggestion;

    private DateMatcher(boolean withYears, boolean withMonth, boolean withDay, Calendar dateSuggestion) {
        this.withYears = withYears;
        this.withMonth = withMonth;
        this.withDay = withDay;
        this.dateSuggestion = dateSuggestion;
    }

    public boolean isWithYears() {
        return withYears;
    }

    public void setWithYears(boolean withYears) {
        this.withYears = withYears;
    }

    public boolean isWithMonth() {
        return withMonth;
    }

    public void setWithMonth(boolean withMonth) {
        this.withMonth = withMonth;
    }

    public boolean isWitDay() {
        return withDay;
    }

    public void setWitDay(boolean witDay) {
        withDay = witDay;
    }

    public Calendar getDateSuggestion() {
        return dateSuggestion;
    }

    public boolean hasMatch() {
        return getDateSuggestion() != null;
    }

    public static Matcher parsingDate(Pattern pattern, String input) {
        return pattern.matcher(input.trim());
    }

    public static DateMatcher fromInput(String input) {
        try {
            return doFromInput(input);
        } catch (NumberFormatException e) {
            return new DateMatcher(false, false, false, null);
        }
    }

    public static DateMatcher doFromInput(String input) {
        Matcher matcher = parsingDate(YEAR_ONLY_MATCHER, input);

        if (matcher.find()) {
            return new DateMatcher(true, false, false, dateToInstance(Integer.parseInt(matcher.group()), 1, 1));
        }
        matcher = parsingDate(MONTH_DIGIT_ONLY_MATCHER, input);
        if (matcher.find()) {
            int month = Integer.parseInt(matcher.group());
            if (month > 12 || month < 1) {
                return new DateMatcher(false, true, false, null);
            }
            return new DateMatcher(false, true, false,
                    dateToInstance(Calendar.getInstance().get(Calendar.YEAR), month, 1));
        }
        matcher = parsingDate(YEAR_MONTHS_MATCHER, input);
        if (matcher.find()) {
            int month = Integer.parseInt(matcher.group().substring(5));
            if (month > 12 || month < 1) {
                return new DateMatcher(true, true, false, null);
            }
            int year = Integer.parseInt(matcher.group().substring(0, 4));

            return new DateMatcher(true, true, false, dateToInstance(year, month, 1));
        }
        matcher = parsingDate(MONTHS_YEAR_MATCHER, input);
        if (matcher.find()) {
            int month = Integer.parseInt(matcher.group().substring(0, 2));
            if (month > 12 || month < 1) {
                return new DateMatcher(true, true, false, null);
            }
            int year = Integer.parseInt(matcher.group().substring(3));

            return new DateMatcher(true, true, false, dateToInstance(year, month, 1));

        }
        matcher = parsingDate(MONTHS_DAY_YEAR_MATCHER, input);
        if (matcher.find()) {
            int first = Integer.parseInt(matcher.group().substring(0, 2));
            int second = Integer.parseInt(matcher.group().substring(3, 5));
            int year = Integer.parseInt(matcher.group().substring(6));
            int control = first + second;
            if (control < 2 || control > 12 + 31 || first < 1 || second < 1) {
                return new DateMatcher(true, true, true, null);
            } else if (control < 12 + 12 + 1) {
                return new DateMatcher(true, true, true, dateToInstance(year, first, second));
            }
            int month = first;
            int day = second;
            if (first > second) {
                month = second;
                day = first;
            }
            Calendar dateToInstance = dateToInstance(year, month, day);
            return new DateMatcher(true, true, true, dateToInstance);
        }
        matcher = parsingDate(YEAR_MONTHS_DAY_MATCHER, input);
        if (matcher.find()) {
            int year = Integer.parseInt(matcher.group().substring(0, 4));
            int first = Integer.parseInt(matcher.group().substring(5, 7));
            int second = Integer.parseInt(matcher.group().substring(8));
            int control = first + second;
            if (control < 2 || control > 12 + 31 || first < 1 || second < 1) {
                return new DateMatcher(true, true, true, null);
            } else if (control < 12 + 12 + 1) {
                return new DateMatcher(true, true, true, dateToInstance(year, first, second));
            }
            int month = first;
            int day = second;
            if (first > second) {
                month = second;
                day = first;
            }
            Calendar dateToInstance = dateToInstance(year, month, day);
            return new DateMatcher(true, true, true, dateToInstance);
        }

        return new DateMatcher(false, false, false, null);
    }

    protected static Calendar dateToInstance(int year, int month, int day) {
        try {
            return GregorianCalendar.from(ZonedDateTime.of(year, month, day, 12, 0, 0, 0, ZoneOffset.UTC));
        } catch (IllegalArgumentException | DateTimeException e) {
            return null;
        }
    }

}
