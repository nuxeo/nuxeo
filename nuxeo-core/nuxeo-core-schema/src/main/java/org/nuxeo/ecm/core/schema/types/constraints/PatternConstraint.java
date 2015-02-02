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
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * This constraint ensures some object's String representation match a pattern.
 *
 * @since 7.1
 */
public class PatternConstraint extends AbstractConstraint {

    private static final long serialVersionUID = 1L;

    private static final String NAME = "PatternConstraint";

    private static final String PNAME_PATTERN = "Pattern";

    protected final Pattern pattern;

    public PatternConstraint(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    @Override
    public boolean validate(Object object) {
        if (object == null) {
            return true;
        }
        return pattern.matcher(object.toString()).matches();
    }

    /**
     * <p>
     * Here, value is : <br>
     * name = {@value #NAME} <br>
     * parameters =
     * <ul>
     * <li>{@value #PNAME_PATTERN} : [0-9]+</li>
     * </ul>
     * </p>
     */
    @Override
    public Description getDescription() {
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put(PNAME_PATTERN, pattern.pattern());
        return new Description(PatternConstraint.NAME, params);
    }

    /**
     * @return The pattern used by this constraint to validate.
     * @since 7.1
     */
    public String getPattern() {
        return pattern.pattern();
    }

    @Override
    public String getErrorMessage(Object invalidValue, Locale locale) {
        // test whether there's a custom translation for this field constraint specific translation
        // the expected key is label.schema.constraint.violation.[ConstraintName]
        // follow the AbstractConstraint behavior otherwise
        List<String> pathTokens = new ArrayList<String>();
        pathTokens.add(MESSAGES_KEY);
        pathTokens.add(PatternConstraint.NAME);
        String key = StringUtils.join(pathTokens, '.');
        Object[] params = new Object[] { getPattern() };
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
        result = prime * result + ((pattern == null) ? 0 : pattern.pattern().hashCode());
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
        PatternConstraint other = (PatternConstraint) obj;
        if (pattern == null) {
            if (other.pattern != null) {
                return false;
            }
        } else if (!pattern.pattern().equals(other.pattern.pattern())) {
            return false;
        }
        return true;
    }

}
