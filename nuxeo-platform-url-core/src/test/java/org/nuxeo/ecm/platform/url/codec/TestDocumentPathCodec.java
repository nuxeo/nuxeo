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
 * $Id: TestDocumentPathCodec.java 28480 2008-01-04 14:04:49Z sfermigier $
 */

package org.nuxeo.ecm.platform.url.codec;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class TestDocumentPathCodec extends TestCase {

    private DocumentView getDocumentView(String id, String path, String view) {
        DocumentLocation docLoc = new DocumentLocationImpl("demo",
                new IdRef(id), new PathRef(path));

        Map<String, String> params = new HashMap<String, String>();
        params.put("tabId", "TAB_CONTENT");
        DocumentView docView = new DocumentViewImpl(docLoc, view, params);
        return docView;
    }

    public void testGetUrlFromDocumentView() {
        DocumentPathCodec codec = new DocumentPathCodec();
        String docid = "dbefd5a0-35ee-4ed2-a023-6817714f32cf";

        DocumentView docView = getDocumentView(docid, "/path/to/my/doc",
                "view_documents");
        String url = "nxpath/demo/path/to/my/doc@view_documents?tabId=TAB_CONTENT";
        assertEquals(url, codec.getUrlFromDocumentView(docView));

        // again without leading slash
        docView = getDocumentView(docid, "path/to/my/doc", "view_documents");
        url = "nxpath/demo/path/to/my/doc@view_documents?tabId=TAB_CONTENT";
        assertEquals(url, codec.getUrlFromDocumentView(docView));

        docView = getDocumentView(docid, "/default-domain", "view_documents");
        url = "nxpath/demo/default-domain@view_documents?tabId=TAB_CONTENT";
        assertEquals(url, codec.getUrlFromDocumentView(docView));

        // check root is handled correctly
        docView = getDocumentView(docid, "/", "view_domains");
        url = "nxpath/demo@view_domains?tabId=TAB_CONTENT";
        assertEquals(url, codec.getUrlFromDocumentView(docView));

        // again with dot in view id
        docView = getDocumentView(docid, "path/to/my/doc", "view.documents");
        url = "nxpath/demo/path/to/my/doc@view.documents?tabId=TAB_CONTENT";
        assertEquals(url, codec.getUrlFromDocumentView(docView));

        // again with dot in repo name
        docView = getDocumentView(docid, "path/to/my/doc.withdot",
                "view_documents");
        url = "nxpath/demo/path/to/my/doc.withdot@view_documents?tabId=TAB_CONTENT";
        assertEquals(url, codec.getUrlFromDocumentView(docView));

        // check url max size
        StringBuffer veryLongUrl = new StringBuffer();
        veryLongUrl.append("nxpath/demo/path/to/my/doc");
        while (veryLongUrl.length() <= DocumentPathCodec.URL_MAX_LENGTH) {
            veryLongUrl.append("/doc");
        }

        DocumentView docViewMaxLength = getDocumentView(docid,
                veryLongUrl.toString(), "view_documents");
        url = "nxdoc/demo/dbefd5a0-35ee-4ed2-a023-6817714f32cf/view_documents?tabId=TAB_CONTENT";
        assertEquals(url, codec.getUrlFromDocumentView(docViewMaxLength));

        // with space in doc path, or non-ASCII chars
        docView = getDocumentView(docid, "/path/ca f\u00e9/doc",
                "view_documents");
        url = "nxpath/demo/path/ca%20f%C3%A9/doc@view_documents?tabId=TAB_CONTENT";
        assertEquals(url, codec.getUrlFromDocumentView(docView));

        // with @ character
        docView = getDocumentView(docid, "/path/foo@bar/doc", "view_documents");
        url = "nxpath/demo/path/foo%40bar/doc@view_documents?tabId=TAB_CONTENT";
        assertEquals(url, codec.getUrlFromDocumentView(docView));
    }

    public void testGetDocumentViewFromUrl() {
        DocumentPathCodec codec = new DocumentPathCodec();
        String url = "nxpath/demo/path/to/my/doc@view_documents?tabId=TAB_CONTENT";
        DocumentView docView = codec.getDocumentViewFromUrl(url);

        DocumentLocation docLoc = docView.getDocumentLocation();
        assertEquals("demo", docLoc.getServerName());
        assertEquals(new PathRef("/path/to/my/doc"), docLoc.getDocRef());
        assertEquals("view_documents", docView.getViewId());
        assertNull(docView.getSubURI());

        Map<String, String> params = docView.getParameters();
        assertEquals("TAB_CONTENT", params.get("tabId"));

        url = "nxpath/demo/default-domain@view_documents";
        docView = codec.getDocumentViewFromUrl(url);
        docLoc = docView.getDocumentLocation();
        assertEquals("demo", docLoc.getServerName());
        assertEquals(new PathRef("/default-domain"), docLoc.getDocRef());
        assertEquals("view_documents", docView.getViewId());
        assertNull(docView.getSubURI());

        url = "nxpath/demo/@view_domains";
        docView = codec.getDocumentViewFromUrl(url);
        docLoc = docView.getDocumentLocation();
        assertEquals("demo", docLoc.getServerName());
        assertEquals(new PathRef("/"), docLoc.getDocRef());
        assertEquals("view_domains", docView.getViewId());
        assertNull(docView.getSubURI());

        // again without final slash
        url = "nxpath/demo@view_domains";
        docView = codec.getDocumentViewFromUrl(url);
        docLoc = docView.getDocumentLocation();
        assertEquals("demo", docLoc.getServerName());
        assertEquals(new PathRef("/"), docLoc.getDocRef());
        assertEquals("view_domains", docView.getViewId());
        assertNull(docView.getSubURI());

        // again with dot in view id
        url = "nxpath/demo@view.domains";
        docView = codec.getDocumentViewFromUrl(url);
        docLoc = docView.getDocumentLocation();
        assertEquals("demo", docLoc.getServerName());
        assertEquals(new PathRef("/"), docLoc.getDocRef());
        assertEquals("view.domains", docView.getViewId());
        assertNull(docView.getSubURI());

        // again with dot in repo name
        url = "nxpath/demo.withdot@view_domains";
        docView = codec.getDocumentViewFromUrl(url);
        docLoc = docView.getDocumentLocation();
        assertEquals("demo.withdot", docLoc.getServerName());
        assertEquals(new PathRef("/"), docLoc.getDocRef());
        assertEquals("view_domains", docView.getViewId());
        assertNull(docView.getSubURI());

        // again with dot in doc name
        url = "nxpath/demo/doc.withdot@view_domains";
        docView = codec.getDocumentViewFromUrl(url);
        docLoc = docView.getDocumentLocation();
        assertEquals("demo", docLoc.getServerName());
        assertEquals(new PathRef("/doc.withdot"), docLoc.getDocRef());
        assertEquals("view_domains", docView.getViewId());
        assertNull(docView.getSubURI());

        // with space in doc path, or non-ASCII chars
        url = "nxpath/demo/ca%20f%C3%A9doc@view_domains";
        docView = codec.getDocumentViewFromUrl(url);
        assertNotNull(docView);
        docLoc = docView.getDocumentLocation();
        assertEquals("demo", docLoc.getServerName());
        assertEquals(new PathRef("/ca f\u00e9doc"), docLoc.getDocRef());
        assertEquals("view_domains", docView.getViewId());
        assertNull(docView.getSubURI());

        // with @ character
        url = "nxpath/demo/path/foo%40bar/doc@view_documents?tabId=TAB_CONTENT";
        docView = codec.getDocumentViewFromUrl(url);
        assertNotNull(docView);
        docLoc = docView.getDocumentLocation();
        assertEquals("demo", docLoc.getServerName());
        assertEquals(new PathRef("/path/foo@bar/doc"), docLoc.getDocRef());
        assertEquals("view_documents", docView.getViewId());
        assertNull(docView.getSubURI());

        // with unescaped @ character (happens when Apache proxying forwards
        // us an URL, some % decoding is done)
        url = "nxpath/demo/path/foo@bar/doc@view_documents?tabId=TAB_CONTENT";
        docView = codec.getDocumentViewFromUrl(url);
        assertNotNull(docView);
        docLoc = docView.getDocumentLocation();
        assertEquals("demo", docLoc.getServerName());
        assertEquals(new PathRef("/path/foo@bar/doc"), docLoc.getDocRef());
        assertEquals("view_documents", docView.getViewId());
        assertNull(docView.getSubURI());
    }

}
