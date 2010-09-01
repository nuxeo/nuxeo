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

package org.nuxeo.ecm.platform.forms.layout.demo.test;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.forms.layout.demo.jsf.LayoutDemoURLCodec;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class TestLayoutDemoURLCodec extends TestCase {

    public void testGetUrlFromDocumentView() {
        LayoutDemoURLCodec codec = new LayoutDemoURLCodec();
        codec.setPrefix("applicationPrefix");
        DocumentLocation docLoc = new DocumentLocationImpl(null, null);
        Map<String, String> params = new HashMap<String, String>();
        params.put("tabId", "TAB_CONTENT");
        params.put("conversationId", "3");
        DocumentView docView = new DocumentViewImpl(docLoc, "view_documents",
                params);

        String url = "applicationPrefix/view_documents?tabId=TAB_CONTENT";
        assertEquals(url, codec.getUrlFromDocumentView(docView));
    }

    public void testGetDocumentViewFromGetUrl() {
        LayoutDemoURLCodec codec = new LayoutDemoURLCodec();
        codec.setPrefix("applicationPrefix");
        String url = "applicationPrefix/view_documents?tabId=TAB_CONTENT&conversationId=3";
        DocumentView docView = codec.getDocumentViewFromUrl(url);

        assertEquals("view_documents", docView.getViewId());
        assertNull(docView.getSubURI());

        Map<String, String> params = docView.getParameters();
        assertEquals("TAB_CONTENT", params.get("tabId"));
        assertNull(params.get("conversationId"));
    }

    public void testGetDocumentViewFromPostUrl() {
        LayoutDemoURLCodec codec = new LayoutDemoURLCodec();
        codec.setPrefix("applicationPrefix");
        String url = "applicationPrefix/widgets/stringWidget.faces?tabId=TAB_CONTENT&conversationId=3";
        DocumentView docView = codec.getDocumentViewFromUrl(url);

        assertNull(docView.getViewId());
        assertNull(docView.getSubURI());

        Map<String, String> params = docView.getParameters();
        assertEquals("TAB_CONTENT", params.get("tabId"));
        assertNull(params.get("conversationId"));
    }

}
