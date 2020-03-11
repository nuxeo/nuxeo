/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */
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