/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.web.common;

import java.util.regex.Pattern;

/**
 * Helper class to detect Html5 Dnd compliant browsers based on the User Agent
 * string
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class UserAgentMatcher {

    private static final Pattern UA_FIREFOX_3 = Pattern.compile("^[Mm]ozilla.*[Ff]irefox(/|\\s)?(3\\.[6789].*)");

    private static final Pattern UA_FIREFOX_FROM_4 = Pattern.compile("^[Mm]ozilla.*[Ff]irefox(/|\\s)?(([456789].*)|(1[0123456789].*))");

    private static final Pattern UA_SAFARI_5 = Pattern.compile("^Mozilla.*AppleWebKit.*Version/5.*");

    private static final Pattern UA_CHROME = Pattern.compile("^Mozilla.*AppleWebKit.*Chrom(e|ium)/(1[0123456789]).*");

    private static final Pattern UA_MSIE_67 = Pattern.compile("^Mozilla/4.0 \\(compatible; MSIE [67].[0-9].*");

    private UserAgentMatcher() {
        // Helper class
    }

    public static boolean isFirefox3(String UA) {
        return UA_FIREFOX_3.matcher(UA).matches();
    }

    public static boolean isFirefox4OrMore(String UA) {
        return UA_FIREFOX_FROM_4.matcher(UA).matches();
    }

    public static boolean isSafari5(String UA) {
        return UA_SAFARI_5.matcher(UA).matches();
    }

    public static boolean isChrome(String UA) {
        return UA_CHROME.matcher(UA).matches();
    }

    public static boolean html5DndIsSupported(String UA) {
        return isFirefox3(UA) || isFirefox4OrMore(UA) || isSafari5(UA)
                || isChrome(UA);
    }

    public static boolean isMSIE6or7(String UA) {
        return UA_MSIE_67.matcher(UA).matches();
    }

}
