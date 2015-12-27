/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    public static synchronized boolean validate(String ip) {
        if (pattern == null) {
            pattern = Pattern.compile(IPADDRESS_PATTERN);
        }
        matcher = pattern.matcher(ip);
        return matcher.matches();
    }
}
