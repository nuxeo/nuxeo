/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: TestDocumentIdCodec.java 28480 2008-01-04 14:04:49Z sfermigier $
 */

package org.nuxeo.ecm.platform.url.codec;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class TestDocumentIdCodec {

    @Test
    public void testGetUrlFromDocumentView() {
        DocumentIdCodec codec = new DocumentIdCodec();
        DocumentLocation docLoc = new DocumentLocationImpl("demo", new IdRef("dbefd5a0-35ee-4ed2-a023-6817714f32cf"));
        Map<String, String> params = new HashMap<>();
        params.put("tabId", "TAB_CONTENT");
        DocumentView docView = new DocumentViewImpl(docLoc, "view_documents", params);

        String url = "nxdoc/demo/dbefd5a0-35ee-4ed2-a023-6817714f32cf/view_documents?tabId=TAB_CONTENT";
        assertEquals(url, codec.getUrlFromDocumentView(docView));
    }

    @Test
    public void testGetDocumentViewFromUrl() {
        DocumentIdCodec codec = new DocumentIdCodec();
        String url = "nxdoc/demo/dbefd5a0-35ee-4ed2-a023-6817714f32cf/view_documents?tabId=TAB_CONTENT";
        DocumentView docView = codec.getDocumentViewFromUrl(url);

        DocumentLocation docLoc = docView.getDocumentLocation();
        assertEquals("demo", docLoc.getServerName());
        assertEquals(new IdRef("dbefd5a0-35ee-4ed2-a023-6817714f32cf"), docLoc.getDocRef());
        assertEquals("view_documents", docView.getViewId());
        assertNull(docView.getSubURI());

        Map<String, String> params = docView.getParameters();
        assertEquals("TAB_CONTENT", params.get("tabId"));
    }

    // do the same without view id (optional)
    @Test
    public void testGetDocumentViewFromUrlNoViewId() {
        DocumentIdCodec codec = new DocumentIdCodec();
        String url = "nxdoc/demo/dbefd5a0-35ee-4ed2-a023-6817714f32cf?tabId=TAB_CONTENT";
        DocumentView docView = codec.getDocumentViewFromUrl(url);

        DocumentLocation docLoc = docView.getDocumentLocation();
        assertEquals("demo", docLoc.getServerName());
        assertEquals(new IdRef("dbefd5a0-35ee-4ed2-a023-6817714f32cf"), docLoc.getDocRef());
        assertNull(docView.getViewId());
        assertNull(docView.getSubURI());

        Map<String, String> params = docView.getParameters();
        assertEquals("TAB_CONTENT", params.get("tabId"));
    }

    // test urls wit a sub uri do not match
    @Test
    public void testGetDocumentViewFromUrlWithSubUri() {
        DocumentIdCodec codec = new DocumentIdCodec();
        String url = "nxdoc/demo/dbefd5a0-35ee-4ed2-a023-6817714f32cf/view_documents/whatever?tabId=TAB_CONTENT";
        DocumentView docView = codec.getDocumentViewFromUrl(url);
        assertNull(docView);
    }

    @Test
    public void testGetDocumentViewFromUrlWithJSessionId() {
        DocumentIdCodec codec = new DocumentIdCodec();
        String url = "nxdoc/demo/dbefd5a0-35ee-4ed2-a023-6817714f32cf/view_documents;jsessionid=7CD6F2222BB08134A57BD2098DA16B2C.nuxeo?tabId=TAB_CONTENT";
        DocumentView docView = codec.getDocumentViewFromUrl(url);
        assertNotNull(docView);

        DocumentLocation docLoc = docView.getDocumentLocation();
        assertEquals("demo", docLoc.getServerName());
        assertEquals("view_documents", docView.getViewId());
        Map<String, String> params = docView.getParameters();
        assertEquals("TAB_CONTENT", params.get("tabId"));
    }

}
