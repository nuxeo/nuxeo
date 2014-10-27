/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
@Deploy("org.nuxeo.ecm.platform.url.core")
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
        DocumentView docView = service.getDocumentViewFromUrl("docid",
                "nxdoc/repo/1234/view_documents?tabId=foo", false, baseUrl);
        assertNotNull(docView);
        assertEquals("view_documents", docView.getViewId());
        assertEquals("foo", docView.getParameter("tabId"));
    }

    @Test
    public void testUrlFromDocumentView() {
        DocumentView docView = new DocumentViewImpl(new DocumentLocationImpl(
                "repo", new PathRef("/ws/my/doc")), "view_doc");
        assertEquals("nxpath/repo/ws/my/doc@view_doc",
                service.getUrlFromDocumentView(docView, false, null));
        assertNull(service.getUrlFromDocumentView("docid", docView, false, null));
        docView = new DocumentViewImpl(new DocumentLocationImpl("repo",
                new IdRef("1234")), "view_doc");
        assertEquals("nxdoc/repo/1234/view_doc",
                service.getUrlFromDocumentView("docid", docView, false, null));
        assertEquals("nxdoc/repo/1234/view_doc",
                service.getUrlFromDocumentView("docid", docView, true, null));
        assertEquals("http://foo/bar/nxdoc/repo/1234/view_doc",
                service.getUrlFromDocumentView("docid", docView, true,
                        "http://foo/bar/"));
    }

}