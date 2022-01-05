/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.common.utils;

import java.util.regex.Pattern;

/**
 * Helper class to detect Html5 Dnd compliant browsers based on the User Agent string
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class UserAgentMatcher {

    private static final Pattern UA_FIREFOX_3 = Pattern.compile("^[Mm]ozilla.*[Ff]irefox(/|\\s)?(3\\.[6789].*)");

    private static final Pattern UA_FIREFOX_FROM_4 = Pattern.compile("^[Mm]ozilla.*[Ff]irefox(/|\\s)?(([456789].*)|([1-9][0123456789].*))");

    private static final Pattern UA_SAFARI_FROM_5 = Pattern.compile("^Mozilla.*AppleWebKit.*Version/.*");

    private static final Pattern UA_CHROME = Pattern.compile("^Mozilla.*AppleWebKit.*Chrom(e|ium)/([1-9][0123456789].([0-9.])*)(?: Safari/([0-9.])*)?");

    private static final Pattern UA_MSIE_67 = Pattern.compile("^Mozilla/4.0 \\(compatible; MSIE [67].[0-9]((?!Trident).)*$");

    private static final Pattern UA_MSIE_FROM_10 = Pattern.compile("^Mozilla.*[Tt]rident/[6-9]\\..*");

    private static final Pattern UA_MSEDGE = Pattern.compile("^Mozilla.*Edg(e)?/.*");

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
        return UA_SAFARI_FROM_5.matcher(UA).matches();
    }

    public static boolean isChrome(String UA) {
        return UA_CHROME.matcher(UA).matches();
    }

    public static boolean html5DndIsSupported(String UA) {
        return isFirefox3(UA) || isFirefox4OrMore(UA) || isSafari5(UA) || isChrome(UA) || isMSIE10OrMore(UA) || isMSEdge(UA);
    }

    public static boolean isMSIE6or7(String UA) {
        return UA_MSIE_67.matcher(UA).matches();
    }

    /**
     * @since 5.9.5
     */
    public static boolean isMSIE10OrMore(String UA) {
        return UA_MSIE_FROM_10.matcher(UA).matches();
    }

    /**
     * @since 7.4
     */
    public static boolean isMSEdge(String UA) {
        return UA_MSEDGE.matcher(UA).matches();
    }

    public static boolean isHistoryPushStateSupported(String UA) {
        return isFirefox4OrMore(UA) || isSafari5(UA) || isChrome(UA) || isMSIE10OrMore(UA);
    }
}
