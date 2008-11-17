/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: ComponentTagUtils.java 28610 2008-01-09 17:13:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.util;

/**
 * Component tag utils.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public final class ComponentTagUtils {

    // Utility class.
    private ComponentTagUtils() {
    }

    /**
     * Returns true if the specified value conforms to the syntax requirements
     * of a value binding expression.
     *
     * @param value The value to evaluate (not null)
     */
    public static boolean isValueReference(String value) {
        return value.contains("#{")
                && value.indexOf("#{") < value.indexOf('}');
    }

    /**
     * Returns true if the specified value conforms to the syntax requirements
     * of a method binding expression.
     * <p>
     * The method can have parameters and the expression must use parentheses
     * even if no parameters are needed.
     *
     * @param value The value to evaluate (not null)
     */
    public static boolean isMethodReference(String value) {
        boolean isValue = isValueReference(value);
        if (isValue) {
            if (value.contains("(")
                    && value.indexOf("(") < value.indexOf(')')
                    // make sure it's not a function
                    && (!value.contains(":") || value.indexOf(":") > value.indexOf("("))) {
                return true;
            }
        }
        return false;
    }

}
