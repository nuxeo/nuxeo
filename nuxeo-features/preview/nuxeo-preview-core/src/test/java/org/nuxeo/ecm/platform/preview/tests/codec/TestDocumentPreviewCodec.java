/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.preview.tests.codec;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.preview.codec.DocumentPreviewCodec;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.codec.api.DocumentViewCodec;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class TestDocumentPreviewCodec {

    private final DocumentViewCodec documentPreviewCodec = new DocumentPreviewCodec();

    @Test
    public void testGetDocumentView() throws Exception {
        final String uuid = "29942295-8683-4e65-917b-f7e7f98e4ad5";
        final String serverName = "default";
        final String propertyPath = "default";
        final String url = "restAPI/preview/" + serverName + "/" + uuid + "/" + propertyPath + "/";

        DocumentView docView = documentPreviewCodec.getDocumentViewFromUrl(url);

        assertNotNull(docView);
        assertNotNull(docView.getDocumentLocation());
        assertEquals(uuid, docView.getDocumentLocation().getDocRef().toString());
        assertEquals(serverName, docView.getDocumentLocation().getServerName());

        Map<String, String> params = docView.getParameters();
        assertTrue(params.containsValue(propertyPath));
    }

    @Test
    public void testGetUrl() throws Exception {
        final String uuid = "29942295-8683-4e65-917b-f7e7f98e4ad5";
        final String serverName = "default";
        final String propertyPath = "default";
        final String expectedUrl = "restAPI/preview/" + serverName + "/" + uuid + "/" + propertyPath + "/";

        DocumentLocation docLoc = new DocumentLocationImpl(serverName, new IdRef(uuid));
        Map<String, String> params = new HashMap<>();
        params.put("PROPERTY_PATH_KEY", propertyPath);
        DocumentView docView = new DocumentViewImpl(docLoc, null, params);

        String url = documentPreviewCodec.getUrlFromDocumentView(docView);
        assertEquals(expectedUrl, url);
    }

}
