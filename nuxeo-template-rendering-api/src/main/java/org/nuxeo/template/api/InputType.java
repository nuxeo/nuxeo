/*
 * (C) Copyright 2006-20012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.template.api;

import java.util.EnumSet;
import java.util.Date;

/**
 * Enum for types of {@link TemplateInput}
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public enum InputType {

    StringValue(String.class.getSimpleName()), BooleanValue(Boolean.class.getSimpleName()), DateValue(
            Date.class.getSimpleName()), DocumentProperty("source"), PictureProperty("picture"), Content("content");

    private final String value;

    InputType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static InputType getByValue(String value) {
        InputType returnValue = null;
        for (final InputType element : EnumSet.allOf(InputType.class)) {
            if (element.toString().equals(value)) {
                returnValue = element;
            }
        }
        return returnValue;
    }
}
