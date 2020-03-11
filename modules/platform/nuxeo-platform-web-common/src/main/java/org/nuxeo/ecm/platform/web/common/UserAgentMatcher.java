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

package org.nuxeo.ecm.platform.web.common;

/**
 * Helper class to detect Html5 Dnd compliant browsers based on the User Agent string
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @deprecated since 7.1, use {@link org.nuxeo.common.utils.UserAgentMatcher} instead.
 */
@Deprecated
public class UserAgentMatcher {

    private UserAgentMatcher() {
        // Helper class
    }

    public static boolean isFirefox3(String UA) {
        return org.nuxeo.common.utils.UserAgentMatcher.isFirefox3(UA);
    }

    public static boolean isFirefox4OrMore(String UA) {
        return org.nuxeo.common.utils.UserAgentMatcher.isFirefox4OrMore(UA);
    }

    public static boolean isSafari5(String UA) {
        return org.nuxeo.common.utils.UserAgentMatcher.isSafari5(UA);
    }

    public static boolean isChrome(String UA) {
        return org.nuxeo.common.utils.UserAgentMatcher.isChrome(UA);
    }

    public static boolean html5DndIsSupported(String UA) {
        return org.nuxeo.common.utils.UserAgentMatcher.html5DndIsSupported(UA);
    }

    public static boolean isMSIE6or7(String UA) {
        return org.nuxeo.common.utils.UserAgentMatcher.isMSIE6or7(UA);
    }

    /**
     * @since 5.9.5
     */
    public static boolean isMSIE10OrMore(String UA) {
        return org.nuxeo.common.utils.UserAgentMatcher.isMSIE10OrMore(UA);
    }

    public static boolean isHistoryPushStateSupported(String UA) {
        return org.nuxeo.common.utils.UserAgentMatcher.isHistoryPushStateSupported(UA);
    }
}
