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
 */
package org.nuxeo.ecm.platform.preview.tests.helper;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.platform.preview.helper.PreviewHelper;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * Tests url generation/resolution via the static helper
 *
 * @author tiry
 */
public class TestHelper extends NXRuntimeTestCase {

    private static final String uuid = "f53fc32e-21b3-4640-9917-05e873aa1e53";
    private static final String targetURL1 = "restAPI/preview/default/f53fc32e-21b3-4640-9917-05e873aa1e53/default/";
    private static final String targetURL2 = "restAPI/preview/default/f53fc32e-21b3-4640-9917-05e873aa1e53/file:content/";

    public void testPreviewURLDefault() {
        DocumentModel doc = new DocumentModelImpl(
                "", "File", uuid, new Path("/"), null, null, null, null, null, null, "default");

        String previewURL = PreviewHelper.getPreviewURL(doc);
        assertNotNull(previewURL);
        assertEquals(targetURL1, previewURL);
    }

    public void testPreviewURL() {
        DocumentModel doc = new DocumentModelImpl(
                "", "File", uuid, new Path("/"), null, null, null, null, null, null, "default");

        String previewURL = PreviewHelper.getPreviewURL(doc, "file:content");
        assertNotNull(previewURL);
        assertEquals(targetURL2, previewURL);
    }

    public void testResolveURLDefault() {
        DocumentRef docRef = PreviewHelper.getDocumentRefFromPreviewURL(targetURL1);
        assertNotNull(docRef);
        assertEquals(uuid, docRef.toString());
    }

}
