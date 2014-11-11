/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: TestURIUtils.java 29987 2008-02-07 22:19:33Z sfermigier $
 */

package org.nuxeo.common.utils;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class TestURIUtils {

    private static final String URI_QUERY
            = "currentTab=TAB_CONTENT&documentId=4012a2d7-384e-4735-ab98-b06b598072fa&repositoryName=demo";

    private static final String PARTIAL_URI
            = "nuxeo/view_documents.faces?" + URI_QUERY;

    private static final String URI = "http://localhost:8080/" + PARTIAL_URI;

    private Map<String, String> parameters;

    @Before
    public void setUp() throws Exception {
        parameters = new HashMap<String, String>();
        parameters.put("currentTab", "TAB_CONTENT");
        parameters.put("documentId", "4012a2d7-384e-4735-ab98-b06b598072fa");
        parameters.put("repositoryName", "demo");
    }

    @Test
    public void testGetRequestParameters() {
        assertEquals(parameters, URIUtils.getRequestParameters(URI_QUERY));
    }

    @Test
    public void testAddParametersToURIQuery() {
        Map<String, String> newParams = new HashMap<String, String>();
        newParams.put("conversationId", "0NXMAIN21");

        Map<String, String> expectedParams = new HashMap<String, String>(parameters);
        expectedParams.put("conversationId", "0NXMAIN21");

        // Test full URI first
        String newUri = URIUtils.addParametersToURIQuery(URI, newParams);

        String uriPath = URIUtils.getURIPath(newUri);
        String newUriQuery = newUri.substring(uriPath.length() + 1);
        Map<String, String> actualParams = URIUtils.getRequestParameters(newUriQuery);

        assertEquals(expectedParams, actualParams);

        // Then test partial URI
        String newPartialUri = URIUtils.addParametersToURIQuery(PARTIAL_URI, newParams);

        uriPath = URIUtils.getURIPath(newUri);
        newUriQuery = newUri.substring(uriPath.length() + 1);
        actualParams = URIUtils.getRequestParameters(newUriQuery);

        assertEquals(expectedParams, actualParams);
    }

    private static String q(String s, boolean b) {
        return URIUtils.quoteURIPathComponent(s, b);
    }

    private static String q(String s, boolean quoteSlash, boolean quoteAt) {
        return URIUtils.quoteURIPathComponent(s, quoteSlash, quoteAt);
    }

    @Test
    public void testQuoteURIPathComponent() {
        assertEquals("test%20yes%3Ano%20%2Fcaf%C3%A9.bin", q(
                "test yes:no /caf\u00e9.bin", true));
        assertEquals("http%3A%2F%2Ffoo%2Fbar", q("http://foo/bar", true));
        assertEquals("a/b/c", q("a/b/c", false));
        // NXP-2480
        assertEquals("%5Bfoo%5D%20bar%3F", q("[foo] bar?", true));
        assertEquals("http%3A%2F%2Ffoo%2Fbar%2F%40adapter", q("http://foo/bar/@adapter", true));
        assertEquals("http%3A%2F%2Ffoo%2Fbar%2F@adapter", q("http://foo/bar/@adapter", true, false));
        assertEquals("http%3A//foo/bar/@adapter", q("http://foo/bar/@adapter", false, false));
    }

    private static String uq(String s) {
        return URIUtils.unquoteURIPathComponent(s);
    }

    @Test
    public void testUnquoteURIPathComponent() {
        assertEquals("test yes:no /caf\u00e9.bin",
                uq("test%20yes%3Ano%20%2Fcaf%C3%A9.bin"));
        assertEquals("http://foo/bar", uq("http%3A%2F%2Ffoo%2Fbar"));
        assertEquals("a/b/c", uq("a/b/c"));
        // NXP-2480
        assertEquals("[foo] bar?", uq("%5Bfoo%5D%20bar%3F"));
    }

}
