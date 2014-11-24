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
 *     Vincent Vergnolle
 */
package org.nuxeo.ecm.platform.picture.convert;

import java.io.Serializable;

/**
 * Shared utils methods for the converters
 *
 * @since 7.1
 *
 * @author Vincent Vergnolle
 */
final class ConverterUtils {

    private ConverterUtils() {
    }

    static int getInteger(Serializable value) {
        if (value instanceof Integer) {
            return (Integer) value;
        } else {
            return (value == null) ? 0 : Integer.valueOf(value.toString());
        }
    }
}
