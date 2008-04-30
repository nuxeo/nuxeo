package org.nuxeo.ecm.platform.rendering.test;

import junit.framework.TestCase;
import org.nuxeo.ecm.platform.rendering.wiki.filters.PatternFilter;

public class TestPatternFilter extends TestCase {

    public void test1() {
        PatternFilter filter = new PatternFilter("[A-Z]+[a-z]+[A-Z][A-Za-z]*", "<link>$0</link>");
        assertEquals("<link>MyName</link>", filter.apply("MyName"));
    }

    public void test2() {
        PatternFilter filter = new PatternFilter(
                "NXP-[0-9]+", "<a href=\"http://jira.nuxeo.org/browse/$0\">$0</a>");
        assertEquals("<a href=\"http://jira.nuxeo.org/browse/NXP-1234\">NXP-1234</a>",
                filter.apply("NXP-1234"));
        assertNull(filter.apply("NXP1234"));
    }

}
