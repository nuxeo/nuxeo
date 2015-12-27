/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Utils {

    private Utils() {
    }

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
            str = new StringBuilder().append(Character.toUpperCase(c)).append(str.substring(1)).toString();
        }
        return str;
    }

    public static String fcToLowerCase(String str) {
        char c = str.charAt(0);
        if (Character.isUpperCase(c)) {
            str = new StringBuilder().append(Character.toLowerCase(c)).append(str.substring(1)).toString();
        }
        return str;
    }

}
