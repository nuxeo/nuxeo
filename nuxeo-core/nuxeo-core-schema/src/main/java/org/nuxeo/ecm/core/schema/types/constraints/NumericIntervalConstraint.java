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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * This constraint ensures a numeric is in an interval.
 * <p>
 * This constraint can validate any {@link Number}.
 * </p>
 *
 * @since 7.1
 */
public class NumericIntervalConstraint extends AbstractConstraint {

    private static final long serialVersionUID = 3630463971175189087L;

    private static final String NAME = "NumericIntervalConstraint";

    private static final String PNAME_MINIMUM = "Minimum";

    private static final String PNAME_MAXIMUM = "Maximum";

    private static final String PNAME_MIN_INC = "MinimumInclusive";

    private static final String PNAME_MAX_INC = "MaximumInclusive";

    private final BigDecimal min;

    private final BigDecimal max;

    private final boolean includingMin;

    private final boolean includingMax;

    /**
     * Use null value to disable a bound.
     * <p>
     * Bounds could be any object having toString representating a number.
     * </p>
     *
     * @param min The lower bound of the interval
     * @param includingMin true if the lower bound is included in the interval
     * @param max The upper bound of the interval
     * @param includingMax true if the upper bound is included in the interval
     */
    public NumericIntervalConstraint(Object min, boolean includingMin, Object max, boolean includingMax) {
        this.min = ConstraintUtils.objectToBigDecimal(min);
        this.includingMin = includingMin;
        this.max = ConstraintUtils.objectToBigDecimal(max);
        this.includingMax = includingMax;
    }

    @Override
    public boolean validate(Object object) {
        BigDecimal val = ConstraintUtils.objectToBigDecimal(object);
        if (val == null) {
            return true;
        }
        if (min != null) {
            int test = min.compareTo(val);
            if (test > 0) {
                return false;
            }
            if (!includingMin && test == 0) {
                return false;
            }
        }
        if (max != null) {
            int test = max.compareTo(val);
            if (test < 0) {
                return false;
            }
            if (!includingMax && test == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Here, value is : <br>
     * name = {@value #NAME} <br>
     * parameters =
     * <ul>
     * <li>{@value #PNAME_MINIMUM} : -125.87 // only if bounded</li>
     * <li>{@value #PNAME_MIN_INC} : true // only if bounded</li>
     * <li>{@value #PNAME_MAXIMUM} : 232 // only if bounded</li>
     * <li>{@value #PNAME_MAX_INC} : false // only if bounded</li>
     * </ul>
     */
    @Override
    public Description getDescription() {
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        if (min != null) {
            params.put(PNAME_MINIMUM, min);
            params.put(PNAME_MIN_INC, includingMin);
        }
        if (max != null) {
            params.put(PNAME_MAXIMUM, max);
            params.put(PNAME_MAX_INC, includingMax);
        }
        return new Description(NumericIntervalConstraint.NAME, params);
    }

    public BigDecimal getMin() {
        return min;
    }

    public BigDecimal getMax() {
        return max;
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
        Object[] params;
        String subKey = (min != null ? (includingMin ? "minin" : "minex") : "")
                + (max != null ? (includingMax ? "maxin" : "maxex") : "");
        if (min != null && max != null) {
            params = new Object[] { min, max };
        } else if (min != null) {
            params = new Object[] { min };
        } else {
            params = new Object[] { max };
        }
        List<String> pathTokens = new ArrayList<String>();
        pathTokens.add(MESSAGES_KEY);
        pathTokens.add(NumericIntervalConstraint.NAME);
        pathTokens.add(subKey);
        String key = StringUtils.join(pathTokens, '.');
        Locale computedLocale = locale != null ? locale : Constraint.MESSAGES_DEFAULT_LANG;
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
        result = prime * result + ((max == null) ? 0 : max.hashCode());
        result = prime * result + ((min == null) ? 0 : min.hashCode());
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
        NumericIntervalConstraint other = (NumericIntervalConstraint) obj;
        if (includingMax != other.includingMax) {
            return false;
        }
        if (includingMin != other.includingMin) {
            return false;
        }
        if (max == null) {
            if (other.max != null) {
                return false;
            }
        } else if (!max.equals(other.max)) {
            return false;
        }
        if (min == null) {
            if (other.min != null) {
                return false;
            }
        } else if (!min.equals(other.min)) {
            return false;
        }
        return true;
    }

}
