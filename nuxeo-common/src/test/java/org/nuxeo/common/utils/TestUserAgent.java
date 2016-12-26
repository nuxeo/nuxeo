/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

public class TestUserAgent {

    private static final Log log = LogFactory.getLog(TestUserAgent.class);

    public static final String MSIE6_UA = "Mozilla/4.0 (compatible; MSIE 6.1;"
            + " Windows XP; .NET CLR 1.1.4322; .NET CLR 2.0.50727)";

    public static final String MSIE7_UA = "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.1;"
            + " WOW64; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729;"
            + " .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C)";

    public static final String MSIE9_COMPATIBILITY_VIEW_UA = "Mozilla/4.0 (compatible; MSIE 7.0;"
            + " Windows NT 6.1; WOW64; Trident/5.0; SLCC2; .NET CLR 2.0.50727;"
            + " .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; " + ".NET4.0C)";

    public static final String MSIE10 = "Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.1; WOW64; Trident/6.0)";

    public static final String MSIE10_COMPAT = "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.1; Trident/6.0)";

    public static final String MSIE11 = "Mozilla/5.0 (Windows NT 6.3; Trident/7.0; rv:11.0) like Gecko";

    public static final String MSIE11_COMPAT = "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.3; Trident/7.0; .NET4.0E; .NET4.0C)";

    public static final String FF_30 = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:30.0) Gecko/20100101 Firefox/30.0";

    public static final String MS_EDGE_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36 Edge/12.<OS build number>";

    @Test
    public void testSupportedBrowsers() throws Exception {

        List<String> UAs = IOUtils
                .readLines(this.getClass().getClassLoader().getResourceAsStream("supportedBrowsers.txt"));
        List<String> BadUAs = IOUtils
                .readLines(this.getClass().getClassLoader().getResourceAsStream("unsupportedBrowsers.txt"));

        for (String UA : UAs) {
            if (!UA.startsWith("#") && !UA.isEmpty()) {
                log.debug("Testing user agent : " + UA);
                assertTrue(UserAgentMatcher.html5DndIsSupported(UA));
            }
        }

        for (String UA : BadUAs) {
            if (!UA.startsWith("#") && !UA.isEmpty()) {
                log.debug("Testing bad user agent : " + UA);
                assertFalse(UserAgentMatcher.html5DndIsSupported(UA));
            }
        }
    }

    @Test
    public void testMSIE9compatibilityViewMatching() {
        assertTrue(UserAgentMatcher.isMSIE6or7(MSIE6_UA));
        assertTrue(UserAgentMatcher.isMSIE6or7(MSIE7_UA));
        // IE9 in compatibility view shouldn't be treated as IE 6 or 7
        assertFalse(UserAgentMatcher.isMSIE6or7(MSIE9_COMPATIBILITY_VIEW_UA));
    }

    @Test
    public void testHistoryPushStateSupport() {
        assertFalse(UserAgentMatcher.isHistoryPushStateSupported(MSIE6_UA));
        assertFalse(UserAgentMatcher.isHistoryPushStateSupported(MSIE7_UA));
        assertFalse(UserAgentMatcher.isHistoryPushStateSupported(MSIE9_COMPATIBILITY_VIEW_UA));
        assertTrue(UserAgentMatcher.isHistoryPushStateSupported(FF_30));
        assertTrue(UserAgentMatcher.isHistoryPushStateSupported(MSIE10));
        assertTrue(UserAgentMatcher.isHistoryPushStateSupported(MSIE10_COMPAT));
        assertTrue(UserAgentMatcher.isHistoryPushStateSupported(MSIE11));
        assertTrue(UserAgentMatcher.isHistoryPushStateSupported(MSIE11_COMPAT));
    }

    @Test
    public void testMSEdge() {
        assertTrue(UserAgentMatcher.isMSEdge(MS_EDGE_UA));
    }

}
