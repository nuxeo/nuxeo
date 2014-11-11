package org.nuxeo.ecm.webapp.dnd;

import java.util.List;

import junit.framework.TestCase;

import org.nuxeo.common.utils.FileUtils;

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
