/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.schema.types.constraints;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This constraint ensures a date is in an interval.
 * <p>
 * This constraint can validate any {@link Date} or {@link Calendar}. This constraint also support {@link Number} types
 * whose long value is recognised as number of milliseconds since January 1, 1970, 00:00:00 GMT. The constraint finally
 * supports String having YYYY-MM-DD format.
 * </p>
 *
 * @since 7.1
 */
public class DateIntervalConstraint extends AbstractConstraint {

    private static final long serialVersionUID = 3630463971175189087L;

    private static final Log log = LogFactory.getLog(DateIntervalConstraint.class);

    private static final String NAME = "DateIntervalConstraint";

    private static final String PNAME_MINIMUM = "Minimum";

    private static final String PNAME_MAXIMUM = "Maximum";

    private static final String PNAME_MIN_INC = "MinimumInclusive";

    private static final String PNAME_MAX_INC = "MaximumInclusive";

    private final Long minTime;

    private final Long maxTime;

    private final boolean includingMin;

    private final boolean includingMax;

    /**
     * Use null value to disable a bound.
     * <p>
     * Bounds could be any {@link Date} or {@link Calendar}. Bounds also support {@link Number} types whose long value
     * is recognised as number of milliseconds since January 1, 1970, 00:00:00 GMT. Bounds finally supports String
     * having YYYY-MM-DD format.
     * </p>
     * <p>
     * Invalid bound (wrong format) would be ignored with log warning.
     * </p>
     *
     * @param minDate The lower bound of the interval
     * @param includingMin true if the lower bound is included in the interval
     * @param maxDate The upper bound of the interval
     * @param includingMax true if the upper bound is included in the interval
     */
    public DateIntervalConstraint(Object minDate, boolean includingMin, Object maxDate, boolean includingMax) {
        minTime = ConstraintUtils.objectToTimeMillis(minDate);
        this.includingMin = includingMin;
        maxTime = ConstraintUtils.objectToTimeMillis(maxDate);
        this.includingMax = includingMax;
        if (minTime != null && maxTime != null && minTime > maxTime) {
            log.warn("lower bound (" + minDate + ") is greater than upper bound (" + maxDate
                    + "). No dates could be valid.");
        }
        if ((minTime == null && minDate != null) || (maxTime == null && maxDate != null)) {
            log.warn("some bound was ignored due to invalid date format (supported format is "
                    + ConstraintUtils.DATE_FORMAT + " (min = " + minDate + " - max = " + maxDate + ")");
        }
    }

    @Override
    public boolean validate(Object object) {
        if (object == null) {
            return true;
        }
        Long timeValue = ConstraintUtils.objectToTimeMillis(object);
        if (timeValue == null) {
            return false;
        }
        if (minTime != null) {
            if (timeValue < minTime.longValue()) {
                return false;
            }
            if (!includingMin && timeValue == minTime.longValue()) {
                return false;
            }
        }
        if (maxTime != null) {
            if (timeValue > maxTime.longValue()) {
                return false;
            }
            if (!includingMax && timeValue == maxTime.longValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Here, value is : <br>
     * name = {@value #NAME}. <br>
     * parameters =
     * <ul>
     * <li>{@value #PNAME_MINIMUM} : 2014-11-05 // only if bounded</li>
     * <li>{@value #PNAME_MIN_INC} : true // only if bounded</li>
     * <li>{@value #PNAME_MAXIMUM} : 2014-11-25 // only if bounded</li>
     * <li>{@value #PNAME_MAX_INC} : false // only if bounded</li>
     * </ul>
     * </p>
     */
    @Override
    public Description getDescription() {
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        if (minTime != null) {
            params.put(PNAME_MINIMUM, new Date(minTime));
            params.put(PNAME_MIN_INC, includingMin);
        }
        if (maxTime != null) {
            params.put(PNAME_MAXIMUM, new Date(maxTime));
            params.put(PNAME_MAX_INC, includingMax);
        }
        return new Description(DateIntervalConstraint.NAME, params);
    }

    public Long getMinTime() {
        return minTime;
    }

    public Long getMaxTime() {
        return maxTime;
    }

    public boolean isIncludingMin() {
        return includingMin;
    }

    public boolean isIncludingMax() {
        return includingMax;
    }

    @Override
    public String getErrorMessage(Object invalidValue, Locale locale) {
        // test whether there's a custom translation for this field constraint specific translation
        // the expected key is label.schema.constraint.violation.[ConstraintName].mininmaxin ou
        // the expected key is label.schema.constraint.violation.[ConstraintName].minexmaxin ou
        // the expected key is label.schema.constraint.violation.[ConstraintName].mininmaxex ou
        // the expected key is label.schema.constraint.violation.[ConstraintName].minexmaxex ou
        // the expected key is label.schema.constraint.violation.[ConstraintName].minin ou
        // the expected key is label.schema.constraint.violation.[ConstraintName].minex ou
        // the expected key is label.schema.constraint.violation.[ConstraintName].maxin ou
        // the expected key is label.schema.constraint.violation.[ConstraintName].maxex
        // follow the AbstractConstraint behavior otherwise
        Locale computedLocale = locale != null ? locale : Constraint.MESSAGES_DEFAULT_LANG;
        Object[] params;
        String subKey = (minTime != null ? (includingMin ? "minin" : "minex") : "")
                + (maxTime != null ? (includingMax ? "maxin" : "maxex") : "");
        DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM, computedLocale);
        if (minTime != null && maxTime != null) {
            String min = format.format(new Date(minTime));
            String max = format.format(new Date(maxTime));
            params = new Object[] { min, max };
        } else if (minTime != null) {
            String min = format.format(new Date(minTime));
            params = new Object[] { min };
        } else {
            String max = format.format(new Date(maxTime));
            params = new Object[] { max };
        }
        List<String> pathTokens = new ArrayList<String>();
        pathTokens.add(MESSAGES_KEY);
        pathTokens.add(DateIntervalConstraint.NAME);
        pathTokens.add(subKey);
        String key = StringUtils.join(pathTokens, '.');
        String message = getMessageString(MESSAGES_BUNDLE, key, params, computedLocale);
        if (message != null && !message.trim().isEmpty() && !key.equals(message)) {
            // use a custom constraint message if there's one
            return message;
        } else {
            // follow AbstractConstraint behavior otherwise
            return super.getErrorMessage(invalidValue, computedLocale);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (includingMax ? 1231 : 1237);
        result = prime * result + (includingMin ? 1231 : 1237);
        result = prime * result + ((maxTime == null) ? 0 : maxTime.hashCode());
        result = prime * result + ((minTime == null) ? 0 : minTime.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DateIntervalConstraint other = (DateIntervalConstraint) obj;
        if (includingMax != other.includingMax) {
            return false;
        }
        if (includingMin != other.includingMin) {
            return false;
        }
        if (maxTime == null) {
            if (other.maxTime != null) {
                return false;
            }
        } else if (!maxTime.equals(other.maxTime)) {
            return false;
        }
        if (minTime == null) {
            if (other.minTime != null) {
                return false;
            }
        } else if (!minTime.equals(other.minTime)) {
            return false;
        }
        return true;
    }

}
