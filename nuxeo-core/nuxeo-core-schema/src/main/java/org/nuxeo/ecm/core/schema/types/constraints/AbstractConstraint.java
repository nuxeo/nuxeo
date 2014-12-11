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

import static org.apache.commons.lang.StringUtils.join;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.constraints.ConstraintViolation.PathNode;

/**
 * <p>
 * This constraint ensures some date representation is in an enumeration.
 * </p>
 * <p>
 * This constraint can validate any {@link Date} or {@link Calendar}. This constraint also support {@link Number} types
 * whose long value is recognised as number of milliseconds since January 1, 1970, 00:00:00 GMT.
 * </p>
 *
 * @since 7.1
 */
public abstract class AbstractConstraint implements Constraint {

    private static final String MESSAGES_BUNDLE = "messages";

    private static final Locale MESSAGES_DEFAULT_LANG = Locale.ENGLISH;

    private static final String MESSAGES_KEY = "label.schema.constraint.violation";

    private static final long serialVersionUID = 1L;

    @Override
    public final String toString() {
        return getDescription().toString();
    }

    @Override
    public String getErrorMessage(Schema schema, List<PathNode> errorLocation, Object invalidValue, Locale locale) {

        String cDesc = getDescription().getName();
        List<String> pathTokens = new ArrayList<String>();
        for (PathNode node : errorLocation) {
            String name = node.getField().getName().getLocalName();
            pathTokens.add(name);
        }
        String fPath = StringUtils.join(pathTokens, '.');
        String fName = "";
        for (PathNode node : errorLocation) {
            String fieldName = node.getField().getName().getLocalName();
            if (node.isListItem()) {
                fieldName += '[' + node.getIndex() + ']';
            }
            pathTokens.add(fieldName + ' ');
        }
        String sName = schema.getName();

        List<Object> paramsList = new ArrayList<Object>();
        paramsList.add(invalidValue);
        paramsList.add(sName);
        paramsList.add(fName);
        paramsList.add(cDesc);
        for (Serializable val : getDescription().getParameters().values()) {
            paramsList.add(val);
        }
        Object[] params = paramsList.toArray();

        String key, message;

        if (locale != null) {

            // schema+field+constraint custom message
            key = join(new String[] { MESSAGES_KEY, cDesc, sName, fPath }, '.');
            message = I18NUtils.getMessageString(MESSAGES_BUNDLE, key, params, locale);
            if (!key.equals(message)) {
                return message;
            }

            // constraint custom message
            key = join(new String[] { MESSAGES_KEY, cDesc }, '.');
            message = I18NUtils.getMessageString(MESSAGES_BUNDLE, key, params, locale);
            if (!key.equals(message)) {
                return message;
            }

            // generic message
            message = I18NUtils.getMessageString(MESSAGES_BUNDLE, MESSAGES_KEY, params, locale);
            if (!key.equals(message)) {
                return message;
            }

        }

        // if already in the default language : return an hard coded message
        // or if no languages specified
        if (locale == null || MESSAGES_DEFAULT_LANG.equals(locale)) {
            return String.format("The constraint '%s' failed on field '%s.%s' for value %s", cDesc, sName, fPath,
                    invalidValue == null ? "null" : invalidValue.toString());
        }

        return getErrorMessage(schema, errorLocation, invalidValue, MESSAGES_DEFAULT_LANG);
    }

}
