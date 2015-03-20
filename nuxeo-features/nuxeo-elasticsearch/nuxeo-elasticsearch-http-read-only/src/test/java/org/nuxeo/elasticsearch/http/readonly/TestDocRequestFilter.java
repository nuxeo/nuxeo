package org.nuxeo.elasticsearch.http.readonly;

import org.junit.Test;
import org.junit.Assert;

public class TestDocRequestFilter {
    private static final String INDICES = "nxutest";

    private static final String TYPES = "doc";

    @Test
    public void testGet() throws Exception {
        DocRequestFilter filter = new DocRequestFilter(TestSearchRequestFilter.getNonAdminPrincipal(), INDICES, TYPES,
                "123", null);
        Assert.assertEquals(filter.getCheckAccessUrl(), "/nxutest/doc/123?fields=ecm:acl");
        Assert.assertEquals(filter.getUrl(), "/nxutest/doc/123");
    }

    @Test
    public void testGetWithParams() throws Exception {
        DocRequestFilter filter = new DocRequestFilter(TestSearchRequestFilter.getNonAdminPrincipal(), INDICES, TYPES,
                "123", "fields=title,content&pretty");
        Assert.assertEquals(filter.getCheckAccessUrl(), "/nxutest/doc/123?fields=ecm:acl");
        Assert.assertEquals(filter.getUrl(), "/nxutest/doc/123?fields=title,content&pretty");
    }
}