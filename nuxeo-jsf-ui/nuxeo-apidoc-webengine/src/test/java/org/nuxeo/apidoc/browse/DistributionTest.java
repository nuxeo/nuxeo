package org.nuxeo.apidoc.browse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.apidoc.browse.Distribution.VERSION_REGEX;

import java.util.regex.Matcher;

import org.junit.Test;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 8.3
 */
public class DistributionTest {
    @Test
    public void testVersionRegex() {
        assertTrue("1".matches(VERSION_REGEX.toString()));
        assertTrue("1.0".matches(VERSION_REGEX.toString()));
        assertTrue("1.1.3".matches(VERSION_REGEX.toString()));
        assertTrue("1.1-BLAL".matches(VERSION_REGEX.toString()));
        assertTrue("1.1-BLA".matches(VERSION_REGEX.toString()));
        assertTrue("100.10.12-DS".matches(VERSION_REGEX.toString()));
    }

    @Test
    public void testVersionMatches() {
        Matcher matcher = VERSION_REGEX.matcher("1");
        assertTrue(matcher.matches());
        assertEquals("1", matcher.group(1));
        assertEquals(null, matcher.group(2));
        assertEquals(null, matcher.group(3));

        matcher = VERSION_REGEX.matcher("1.10");
        assertTrue(matcher.matches());
        assertEquals("1", matcher.group(1));
        assertEquals("10", matcher.group(2));
        assertEquals(null, matcher.group(3));

        matcher = VERSION_REGEX.matcher("1.2.3");
        assertTrue(matcher.matches());
        assertEquals("1", matcher.group(1));
        assertEquals("2", matcher.group(2));
        assertEquals("3", matcher.group(3));

        matcher = VERSION_REGEX.matcher("1.2-SNAPSHOT");
        assertTrue(matcher.matches());
        assertEquals("1", matcher.group(1));
        assertEquals("2", matcher.group(2));
        assertEquals(null, matcher.group(3));
    }
}
