/*
 * (C) Copyright 2011-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     tdelprat
 *
 */
package org.nuxeo.wizard.helpers;

/**
 * Simple RegExp based Number validator
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.2
 */
public class NumberValidator {

    public static synchronized boolean validate(String ip) {
        try {
            Long.valueOf(ip);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
}
