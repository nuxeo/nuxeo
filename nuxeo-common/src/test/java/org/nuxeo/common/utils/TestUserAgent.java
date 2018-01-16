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

package org.nuxeo.common.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class TestUserAgent {

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

    public static final String SAFARI11 = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_2) AppleWebKit/604.4.7 (KHTML, like Gecko) Version/11.0.2 Safari/604.4.7";

    @Test
    public void testSupportedBrowsers() throws Exception {

        List<String> UAs = FileUtils.readLines(this.getClass().getClassLoader().getResourceAsStream(
                "supportedBrowsers.txt"));
        List<String> BadUAs = FileUtils.readLines(this.getClass().getClassLoader().getResourceAsStream(
                "unsupportedBrowsers.txt"));

        for (String UA : UAs) {
            if (!UA.startsWith("#") && !UA.isEmpty()) {
                System.out.println("Testing user agent : " + UA);
                assertTrue(UserAgentMatcher.html5DndIsSupported(UA));
            }
        }

        for (String UA : BadUAs) {
            if (!UA.startsWith("#") && !UA.isEmpty()) {
                System.out.println("Testing bad user agent : " + UA);
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
