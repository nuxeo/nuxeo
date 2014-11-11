/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: TestURIUtils.java 29987 2008-02-07 22:19:33Z sfermigier $
 */

package org.nuxeo.common.utils;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class TestURIUtils extends TestCase {

    private static final String URI_QUERY = "currentTab=TAB_CONTENT&documentId=4012a2d7-384e-4735-ab98-b06b598072fa&repositoryName=demo";

    private static final String PARTIAL_URI = "nuxeo/view_documents.faces?"
            + URI_QUERY;

    private static final String URI = "http://localhost:8080/" + PARTIAL_URI;

    private Map<String, String> parameters;

    @Override
    protected void setUp() throws Exception {
        parameters = new HashMap<String, String>();
        parameters.put("currentTab", "TAB_CONTENT");
        parameters.put("documentId", "4012a2d7-384e-4735-ab98-b06b598072fa");
        parameters.put("repositoryName", "demo");
    }

    public void testGetRequestParameters() {
        assertEquals(parameters, URIUtils.getRequestParameters(URI_QUERY));
    }

    // FIXME: this tests makes too string assumptions on how the params will be
    // ordered (fails under Java 6)
    public void XXXtestAddParametersToURIQuery() {
        String newUri = "http://localhost:8080/nuxeo/view_documents.faces?currentTab=TAB_CONTENT&documentId=4012a2d7-384e-4735-ab98-b06b598072fa&conversationId=0NXMAIN21&repositoryName=demo";
        Map<String, String> newParams = new HashMap<String, String>();
        newParams.put("conversationId", "0NXMAIN21");
        assertEquals(newUri, URIUtils.addParametersToURIQuery(URI, newParams));
    }

    // FIXME: this tests makes too string assumptions on how the params will be
    // ordered (fails under Java 6)
    public void XXXtestAddParametersToPartialURIQuery() {
        String newUri = "nuxeo/view_documents.faces?currentTab=TAB_CONTENT&documentId=4012a2d7-384e-4735-ab98-b06b598072fa&conversationId=0NXMAIN21&repositoryName=demo";
        Map<String, String> newParams = new HashMap<String, String>();
        newParams.put("conversationId", "0NXMAIN21");
        assertEquals(newUri, URIUtils.addParametersToURIQuery(PARTIAL_URI,
                newParams));
    }

    public void testQuoteURIPathComponent() throws Exception {
        String s = "test yes:no /caf\u00e9.bin";
        assertEquals("test%20yes%3Ano%20%2Fcaf%C3%A9.bin",
                URIUtils.quoteURIPathComponent(s, true));
        s = "http://foo/bar";
        assertEquals("http%3A%2F%2Ffoo%2Fbar", URIUtils.quoteURIPathComponent(
                s, true));
        s = "a/b/c";
        assertEquals("a/b/c", URIUtils.quoteURIPathComponent(s, false));
        // NXP-2480
        s = "[foo] bar?";
        assertEquals("%5Bfoo%5D%20bar%3F", URIUtils.quoteURIPathComponent(s,
                true));
    }

}
