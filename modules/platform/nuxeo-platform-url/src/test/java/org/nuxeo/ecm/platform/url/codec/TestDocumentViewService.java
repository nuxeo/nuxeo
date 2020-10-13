/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.url.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 6.0
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.platform.url")
public class TestDocumentViewService {

    @Inject
    protected DocumentViewCodecManager service;

    @Test
    public void testServiceLookup() {
        assertNotNull(service);
    }

    @Test
    public void testServiceDefaultCodec() {
        assertEquals("docid", service.getDefaultCodecName());
    }

    @Test
    public void testDocumentViewFromUrl() {
        String baseUrl = "http://foo.bar";
        DocumentView docView = service.getDocumentViewFromUrl("docid", "nxdoc/repo/1234/view_documents?tabId=foo",
                false, baseUrl);
        assertNotNull(docView);
        assertEquals("view_documents", docView.getViewId());
        assertEquals("foo", docView.getParameter("tabId"));
    }

    @Test
    public void testUrlFromDocumentView() {
        DocumentView docView = new DocumentViewImpl(new DocumentLocationImpl("repo", new PathRef("/ws/my/doc")),
                "view_doc");
        assertEquals("nxpath/repo/ws/my/doc@view_doc", service.getUrlFromDocumentView(docView, false, null));
        assertNull(service.getUrlFromDocumentView("docid", docView, false, null));
        docView = new DocumentViewImpl(new DocumentLocationImpl("repo", new IdRef("1234")), "view_doc");
        assertEquals("nxdoc/repo/1234/view_doc", service.getUrlFromDocumentView("docid", docView, false, null));
        assertEquals("nxdoc/repo/1234/view_doc", service.getUrlFromDocumentView("docid", docView, true, null));
        assertEquals("http://foo/bar/nxdoc/repo/1234/view_doc",
                service.getUrlFromDocumentView("docid", docView, true, "http://foo/bar/"));
        assertEquals("http://foo/bar/nxdoc/repo/1234/view_doc",
                service.getUrlFromDocumentView("docid", docView, true, "http://foo/bar"));
    }

}
