package org.nuxeo.elasticsearch.http.readonly;

import org.junit.Test;
import org.testng.Assert;

import static org.junit.Assert.*;

public class TestDocRequestFilter {
    private static final String INDICES = "nxutest";

    private static final String TYPES = "doc";

    @Test
    public void testMatchAll() throws Exception {
        DocRequestFilter filter = new DocRequestFilter(TestSearchRequestFilter.getNonAdminPrincipal(), INDICES, TYPES, "123", "pretty");
        Assert.assertEquals("/nxutest/doc/123?pretty", filter.getUrl());
    }

}