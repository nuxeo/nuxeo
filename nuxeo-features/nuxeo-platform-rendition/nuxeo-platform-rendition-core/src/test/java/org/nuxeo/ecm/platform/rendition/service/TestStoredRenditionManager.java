/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.platform.rendition.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.ecm.platform.rendition.Renderable;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

@RunWith(FeaturesRunner.class)
@Features(RenditionFeature.class)
@LocalDeploy({ "org.nuxeo.ecm.platform.rendition.core:test-renditionprovider-contrib.xml",
        "org.nuxeo.ecm.platform.rendition.core:test-stored-rendition-manager-contrib.xml" })
public class TestStoredRenditionManager {

    @Inject
    protected CoreSession session;

    @Inject
    protected RenditionService renditionService;

    @Test
    public void testDummyRendition() throws Exception {
        DocumentModel file = createBlobDoc("File");
        Rendition ren = renditionService.getRendition(file, "dummyRendition", true);
        assertNotNull(ren);
        Blob blob = ren.getBlob();
        assertEquals(file.getPropertyValue("dc:description"), blob.getString());
        assertEquals("dummy/pdf", blob.getMimeType());
    }

    protected DocumentModel createBlobDoc(String typeName) {
        DocumentModel file = session.createDocumentModel("/", "dummy-file", typeName);
        file.setPropertyValue("dc:description", "dummy-description");
        BlobHolder bh = file.getAdapter(BlobHolder.class);
        Blob blob = Blobs.createBlob("Dummy text");
        blob.setFilename("dummy.txt");
        bh.setBlob(blob);
        return session.createDocument(file);
    }

}
