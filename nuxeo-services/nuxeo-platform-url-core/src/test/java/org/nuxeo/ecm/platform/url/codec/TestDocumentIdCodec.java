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
 * $Id: TestDocumentIdCodec.java 28480 2008-01-04 14:04:49Z sfermigier $
 */

package org.nuxeo.ecm.platform.url.codec;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class TestDocumentIdCodec extends TestCase {

    public void testGetUrlFromDocumentView() {
        DocumentIdCodec codec = new DocumentIdCodec();
        DocumentLocation docLoc = new DocumentLocationImpl("demo", new IdRef(
                "dbefd5a0-35ee-4ed2-a023-6817714f32cf"));
        Map<String, String> params = new HashMap<String, String>();
        params.put("tabId", "TAB_CONTENT");
        DocumentView docView = new DocumentViewImpl(docLoc, "view_documents",
                params);

        String url = "nxdoc/demo/dbefd5a0-35ee-4ed2-a023-6817714f32cf/view_documents?tabId=TAB_CONTENT";
        assertEquals(url, codec.getUrlFromDocumentView(docView));
    }

    public void testGetDocumentViewFromUrl() {
        DocumentIdCodec codec = new DocumentIdCodec();
        String url = "nxdoc/demo/dbefd5a0-35ee-4ed2-a023-6817714f32cf/view_documents?tabId=TAB_CONTENT";
        DocumentView docView = codec.getDocumentViewFromUrl(url);

        DocumentLocation docLoc = docView.getDocumentLocation();
        assertEquals("demo", docLoc.getServerName());
        assertEquals(new IdRef("dbefd5a0-35ee-4ed2-a023-6817714f32cf"),
                docLoc.getDocRef());
        assertEquals("view_documents", docView.getViewId());
        assertNull(docView.getSubURI());

        Map<String, String> params = docView.getParameters();
        assertEquals("TAB_CONTENT", params.get("tabId"));
    }

    // do the same without view id (optional)
    public void testGetDocumentViewFromUrlNoViewId() {
        DocumentIdCodec codec = new DocumentIdCodec();
        String url = "nxdoc/demo/dbefd5a0-35ee-4ed2-a023-6817714f32cf?tabId=TAB_CONTENT";
        DocumentView docView = codec.getDocumentViewFromUrl(url);

        DocumentLocation docLoc = docView.getDocumentLocation();
        assertEquals("demo", docLoc.getServerName());
        assertEquals(new IdRef("dbefd5a0-35ee-4ed2-a023-6817714f32cf"),
                docLoc.getDocRef());
        assertNull(docView.getViewId());
        assertNull(docView.getSubURI());

        Map<String, String> params = docView.getParameters();
        assertEquals("TAB_CONTENT", params.get("tabId"));
    }

    // test urls wit a sub uri do not match
    public void testGetDocumentViewFromUrlWithSubUri() {
        DocumentIdCodec codec = new DocumentIdCodec();
        String url = "nxdoc/demo/dbefd5a0-35ee-4ed2-a023-6817714f32cf/view_documents/whatever?tabId=TAB_CONTENT";
        DocumentView docView = codec.getDocumentViewFromUrl(url);
        assertNull(docView);
    }

}
