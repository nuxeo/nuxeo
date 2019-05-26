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

package org.nuxeo.ecm.platform.forms.layout.demo.test;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.forms.layout.demo.jsf.LayoutDemoURLCodec;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class TestLayoutDemoURLCodec {

    @Test
    public void testGetUrlFromDocumentView() {
        LayoutDemoURLCodec codec = new LayoutDemoURLCodec();
        codec.setPrefix("applicationPrefix");
        DocumentLocation docLoc = new DocumentLocationImpl(null, null);
        Map<String, String> params = new HashMap<>();
        params.put("tabId", "TAB_CONTENT");
        params.put("conversationId", "3");
        DocumentView docView = new DocumentViewImpl(docLoc, "view_documents", params);

        String url = "applicationPrefix/view_documents/TAB_CONTENT";
        assertEquals(url, codec.getUrlFromDocumentView(docView));
    }

    @Test
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

    @Test
    public void testGetDocumentViewAndTabsFromGetUrl() {
        LayoutDemoURLCodec codec = new LayoutDemoURLCodec();
        codec.setPrefix("applicationPrefix");
        String url = "applicationPrefix/view_documents/TAB_CONTENT/subTab?conversationId=3";
        DocumentView docView = codec.getDocumentViewFromUrl(url);

        assertEquals("view_documents", docView.getViewId());
        assertNull(docView.getSubURI());

        Map<String, String> params = docView.getParameters();
        assertEquals("TAB_CONTENT", params.get("tabId"));
        assertEquals("subTab", params.get("subTabId"));
        assertNull(params.get("conversationId"));
    }

    /**
     * tests that js, img,... resources are not handled by the coded because tab and sub tab ids holds a "." character
     */
    @Test
    public void testGetDocumentViewFromResourceURL() {
        LayoutDemoURLCodec codec = new LayoutDemoURLCodec();
        codec.setPrefix("applicationPrefix");
        String url = "applicationPrefix/img/icon.png";
        DocumentView docView = codec.getDocumentViewFromUrl(url);
        assertNull(docView);

        url = "layoutDemo/sources/OSGI-INF/demo/layout-demo-text-widget.xml";
        docView = codec.getDocumentViewFromUrl(url);
        assertNull(docView);
    }

    @Test
    public void testGetDocumentViewFromPostUrl() {
        LayoutDemoURLCodec codec = new LayoutDemoURLCodec();
        codec.setPrefix("applicationPrefix");
        String url = "applicationPrefix/demoWidgets/stringWidget.faces?tabId=TAB_CONTENT&conversationId=3";
        DocumentView docView = codec.getDocumentViewFromUrl(url);

        assertNull(docView.getViewId());
        assertNull(docView.getSubURI());

        Map<String, String> params = docView.getParameters();
        assertEquals("TAB_CONTENT", params.get("tabId"));
        assertNull(params.get("conversationId"));
    }

}
