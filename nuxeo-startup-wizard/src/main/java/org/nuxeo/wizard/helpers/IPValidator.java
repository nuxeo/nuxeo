/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     tdelprat
 *
 */

package org.nuxeo.wizard.helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RegExp based helper to check IP address format
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.2
 */
public class IPValidator {

    private static Pattern pattern;

    private static Matcher matcher;

    private static final String IPADDRESS_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    public static synchronized boolean validate(String ip) {
        if (pattern == null) {
            pattern = Pattern.compile(IPADDRESS_PATTERN);
        }
        matcher = pattern.matcher(ip);
        return matcher.matches();
    }
}
