/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.preview.tests.codec;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.preview.codec.DocumentPreviewCodec;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.codec.api.DocumentViewCodec;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class TestDocumentPreviewCodec extends TestCase {

    private final DocumentViewCodec documentPreviewCodec = new DocumentPreviewCodec();

    public void testGetDocumentView() throws Exception {
        final String uuid = "29942295-8683-4e65-917b-f7e7f98e4ad5";
        final String serverName = "default";
        final String propertyPath = "default";
        final String url = "restAPI/preview/" + serverName + "/" + uuid + "/"
                + propertyPath + "/";

        DocumentView docView = documentPreviewCodec.getDocumentViewFromUrl(url);

        assertNotNull(docView);
        assertNotNull(docView.getDocumentLocation());
        assertEquals(uuid, docView.getDocumentLocation().getDocRef().toString());
        assertEquals(serverName,
                docView.getDocumentLocation().getServerName());

        Map<String, String> params = docView.getParameters();
        assertTrue(params.containsValue(propertyPath));
    }

    public void testGetUrl() throws Exception {
        final String uuid = "29942295-8683-4e65-917b-f7e7f98e4ad5";
        final String serverName = "default";
        final String propertyPath = "default";
        final String expectedUrl = "restAPI/preview/" + serverName + "/" + uuid
                + "/" + propertyPath + "/";

        DocumentLocation docLoc = new DocumentLocationImpl(serverName,
                new IdRef(uuid));
        Map<String, String> params = new HashMap<String, String>();
        params.put("PROPERTY_PATH_KEY", propertyPath);
        DocumentView docView = new DocumentViewImpl(docLoc, null, params);

        String url = documentPreviewCodec.getUrlFromDocumentView(docView);
        assertEquals(expectedUrl, url);
    }

}
