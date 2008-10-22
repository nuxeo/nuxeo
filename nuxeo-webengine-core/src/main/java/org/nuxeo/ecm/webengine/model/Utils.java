/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Utils {

    private Utils() {}

    public static boolean streq(String str1, String str2) {
        if (str1 == null) {
            return str2 == null;
        } else if (str2 == null) {
            return false;
        } else {
            return str1.equals(str2);
        }
    }

    public static String nullIfEmpty(String str) {
        return str != null && str.length() == 0 ? null : str;
    }

    public static String fcToUpperCase(String str) {
        char c = str.charAt(0);
        if (Character.isLowerCase(c)) {
            str = new StringBuilder().append(Character.toUpperCase(c))
                    .append(str.substring(1)).toString();
        }
        return str;
    }

    public static String fcToLowerCase(String str) {
        char c = str.charAt(0);
        if (Character.isUpperCase(c)) {
            str = new StringBuilder().append(Character.toLowerCase(c))
                    .append(str.substring(1)).toString();
        }
        return str;
    }

}
