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
 * $Id: TestDocumentFileCodec.java 22839 2007-07-22 20:43:16Z sfermigier $
 */

package org.nuxeo.ecm.platform.url.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class TestDocumentFileCodec {

    @Test
    public void testGetUrlFromDocumentView() {
        DocumentFileCodec codec = new DocumentFileCodec("nxfile");
        DocumentLocation docLoc = new DocumentLocationImpl("demo", new IdRef("dbefd5a0-35ee-4ed2-a023-6817714f32cf"));
        Map<String, String> params = new HashMap<>();
        params.put(DocumentFileCodec.FILE_PROPERTY_PATH_KEY, "file:content");
        params.put(DocumentFileCodec.FILENAME_KEY, "mydoc.odt");
        DocumentView docView = new DocumentViewImpl(docLoc, null, params);

        String url = "nxfile/demo/dbefd5a0-35ee-4ed2-a023-6817714f32cf/file:content/mydoc.odt";
        assertEquals(url, codec.getUrlFromDocumentView(docView));
    }

    // same with spaces in file name
    @Test
    public void testGetUrlFromDocumentViewEncoding() {
        DocumentFileCodec codec = new DocumentFileCodec("nxfile");
        DocumentLocation docLoc = new DocumentLocationImpl("demo", new IdRef("dbefd5a0-35ee-4ed2-a023-6817714f32cf"));
        Map<String, String> params = new HashMap<>();
        params.put(DocumentFileCodec.FILE_PROPERTY_PATH_KEY, "file:content");
        params.put(DocumentFileCodec.FILENAME_KEY, "my doc \u00e9.odt");
        DocumentView docView = new DocumentViewImpl(docLoc, null, params);

        String url = "nxfile/demo/dbefd5a0-35ee-4ed2-a023-6817714f32cf/file:content/my%20doc%20%C3%A9.odt";
        assertEquals(url, codec.getUrlFromDocumentView(docView));
    }

    // same with reserved characters in file name and additional request params
    @Test
    public void testGetUrlFromDocumentViewWithReservedAndParams() {
        DocumentFileCodec codec = new DocumentFileCodec("nxfile");
        DocumentLocation docLoc = new DocumentLocationImpl("demo", new IdRef("dbefd5a0-35ee-4ed2-a023-6817714f32cf"));
        Map<String, String> params = new HashMap<>();
        params.put(DocumentFileCodec.FILE_PROPERTY_PATH_KEY, "file:content");
        params.put(DocumentFileCodec.FILENAME_KEY, "my [doc]? \u00e9.odt");
        params.put("foo", "bar");
        DocumentView docView = new DocumentViewImpl(docLoc, null, params);

        String url = "nxfile/demo/dbefd5a0-35ee-4ed2-a023-6817714f32cf/file:content/my%20%5Bdoc%5D%3F%20%C3%A9.odt?foo=bar";
        assertEquals(url, codec.getUrlFromDocumentView(docView));
    }

    @Test
    public void testGetDocumentViewFromUrl() {
        DocumentFileCodec codec = new DocumentFileCodec();
        codec.setPrefix("nxfile");
        String url = "nxfile/demo/dbefd5a0-35ee-4ed2-a023-6817714f32cf/file:content/mydoc.odt";
        DocumentView docView = codec.getDocumentViewFromUrl(url);

        DocumentLocation docLoc = docView.getDocumentLocation();
        assertEquals("demo", docLoc.getServerName());
        assertEquals(new IdRef("dbefd5a0-35ee-4ed2-a023-6817714f32cf"), docLoc.getDocRef());
        assertNull(docView.getViewId());
        assertNull(docView.getSubURI());

        Map<String, String> params = docView.getParameters();
        assertEquals("file:content", params.get(DocumentFileCodec.FILE_PROPERTY_PATH_KEY));
        assertEquals("mydoc.odt", params.get(DocumentFileCodec.FILENAME_KEY));
    }

    @Test
    public void testGetDocumentViewFromUrlWithJsessionid() {
        DocumentFileCodec codec = new DocumentFileCodec("nxfile");
        String url = "nxfile/demo/dbefd5a0-35ee-4ed2-a023-6817714f32cf/file:content/mydoc.odt;jsessionid=38DE2293806643550EB569D8EB827219.nuxeo";
        DocumentView docView = codec.getDocumentViewFromUrl(url);

        DocumentLocation docLoc = docView.getDocumentLocation();
        assertEquals("demo", docLoc.getServerName());
        assertEquals(new IdRef("dbefd5a0-35ee-4ed2-a023-6817714f32cf"), docLoc.getDocRef());
        assertNull(docView.getViewId());
        assertNull(docView.getSubURI());

        Map<String, String> params = docView.getParameters();
        assertEquals("file:content", params.get(DocumentFileCodec.FILE_PROPERTY_PATH_KEY));
        assertEquals("mydoc.odt", params.get(DocumentFileCodec.FILENAME_KEY));
    }

    // same with spaces in file name
    @Test
    public void testGetDocumentViewFromUrlDecoding() {
        DocumentFileCodec codec = new DocumentFileCodec("nxfile");
        String url = "nxfile/demo/dbefd5a0-35ee-4ed2-a023-6817714f32cf/file:content/my%20doc%20%C3%A9.odt";
        DocumentView docView = codec.getDocumentViewFromUrl(url);

        DocumentLocation docLoc = docView.getDocumentLocation();
        assertEquals("demo", docLoc.getServerName());
        assertEquals(new IdRef("dbefd5a0-35ee-4ed2-a023-6817714f32cf"), docLoc.getDocRef());
        assertNull(docView.getViewId());
        assertNull(docView.getSubURI());

        Map<String, String> params = docView.getParameters();
        assertEquals("file:content", params.get(DocumentFileCodec.FILE_PROPERTY_PATH_KEY));
        assertEquals("my doc \u00e9.odt", params.get(DocumentFileCodec.FILENAME_KEY));
    }

    // same with reserved characters in file name and params
    @Test
    public void testGetDocumentViewFromUrlWithReservedAndParams() {
        DocumentFileCodec codec = new DocumentFileCodec("nxfile");
        String url = "nxfile/demo/dbefd5a0-35ee-4ed2-a023-6817714f32cf/file:content/my%20%5Bdoc%5D%3F%20%C3%A9.odt?foo=bar";
        DocumentView docView = codec.getDocumentViewFromUrl(url);

        DocumentLocation docLoc = docView.getDocumentLocation();
        assertEquals("demo", docLoc.getServerName());
        assertEquals(new IdRef("dbefd5a0-35ee-4ed2-a023-6817714f32cf"), docLoc.getDocRef());
        assertNull(docView.getViewId());
        assertNull(docView.getSubURI());

        Map<String, String> params = docView.getParameters();
        assertEquals("file:content", params.get(DocumentFileCodec.FILE_PROPERTY_PATH_KEY));
        assertEquals("my [doc]? \u00e9.odt", params.get(DocumentFileCodec.FILENAME_KEY));
        assertEquals("bar", params.get("foo"));
    }

}
