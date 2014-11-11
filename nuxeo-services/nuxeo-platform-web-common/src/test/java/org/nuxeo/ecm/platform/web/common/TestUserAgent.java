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

import java.util.List;

import org.nuxeo.common.utils.FileUtils;

import junit.framework.TestCase;

public class TestUserAgent extends TestCase {


    public void testSupportedBrowsers() throws Exception {

        List<String> UAs = FileUtils.readLines(this.getClass().getClassLoader().getResourceAsStream("supportedBrowsers.txt"));
        List<String> BadUAs = FileUtils.readLines(this.getClass().getClassLoader().getResourceAsStream("unsupportedBrowsers.txt"));

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
}
