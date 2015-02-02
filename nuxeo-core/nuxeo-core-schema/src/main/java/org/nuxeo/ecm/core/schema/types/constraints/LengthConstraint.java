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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * This constraints checks whether an object's String representation size is in some interval.
 * <p>
 * This constraint's bounds are not strict (i.e. >= and <=).
 * </p>
 *
 * @since 7.1
 */
public class LengthConstraint extends AbstractConstraint {

    private static final long serialVersionUID = 3630463971175189087L;

    private static final String NAME = "LengthConstraint";

    private static final String PNAME_MIN_LENGTH = "Minimum";

    private static final String PNAME_MAX_LENGTH = "Maximum";

    private final Long min;

    private final Long max;

    /**
     * For a fixed length, use min = max values.
     * <p>
     * Bounds could be any object having toString representating an integer.
     * </p>
     *
     * @param min Minimum length for the validated String, use a null value to get unbounded length.
     * @param max Maximum length for the validated String, use a null value to get unbounded length.
     */
    public LengthConstraint(Object min, Object max) {
        this.min = ConstraintUtils.objectToPostiveLong(min);
        this.max = ConstraintUtils.objectToPostiveLong(max);
    }

    @Override
    public boolean validate(Object object) {
        if (object == null) {
            return true;
        }
        int len = object.toString().length();
        if (min != null && len < min) {
            return false;
        }
        if (max != null && len > max) {
            return false;
        }
        return true;
    }

    /**
     * Here, value is : <br>
     * name = {@value #NAME} <br>
     * parameters =
     * <ul>
     * <li>{@value #PNAME_MIN_LENGTH} : 5 // only is bounded</li>
     * <li>{@value #PNAME_MAX_LENGTH} : 10 // only if bounded</li>
     * </ul>
     * </p>
     */
    @Override
    public Description getDescription() {
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        if (min != null) {
            params.put(PNAME_MIN_LENGTH, min);
        }
        if (max != null) {
            params.put(PNAME_MAX_LENGTH, max);
        }
        return new Description(LengthConstraint.NAME, params);
    }

    /**
     * @return This constraints minimum length if bounded, null otherwise.
     * @since 7.1
     */
    public Long getMin() {
        return min;
    }

    /**
     * @return This constraints maximum length if bounded, null otherwise.
     * @since 7.1
     */
    public Long getMax() {
        return max;
    }

    @Override
    public String getErrorMessage(Object invalidValue, Locale locale) {
        // test whether there's a custom translation for this field constraint specific translation
        // the expected key is label.schema.constraint.violation.[ConstraintName].minmax ou
        // the expected key is label.schema.constraint.violation.[ConstraintName].min ou
        // the expected key is label.schema.constraint.violation.[ConstraintName].max
        // follow the AbstractConstraint behavior otherwise
        Object[] params;
        String subKey;
        if (min != null && max != null) {
            params = new Object[] { min, max };
            subKey = "minmax";
        } else if (min != null) {
            params = new Object[] { min };
            subKey = "min";
        } else {
            params = new Object[] { max };
            subKey = "max";
        }
        List<String> pathTokens = new ArrayList<String>();
        pathTokens.add(MESSAGES_KEY);
        pathTokens.add(LengthConstraint.NAME);
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
        LengthConstraint other = (LengthConstraint) obj;
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
