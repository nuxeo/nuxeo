/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.rendition.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.rendition.Renderable;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(type = BackendType.H2, init = DefaultRepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.core.convert.api", "org.nuxeo.ecm.core.convert",
        "org.nuxeo.ecm.core.convert.plugins", "org.nuxeo.ecm.platform.convert",
        "org.nuxeo.ecm.platform.rendition.api",
        "org.nuxeo.ecm.platform.rendition.core",
        "org.nuxeo.ecm.automation.core" })
@LocalDeploy({ "org.nuxeo.ecm.platform.rendition.core:test-renditionprovider-contrib.xml" })
public class TestRenditionProvider {

    public static final String PDF_RENDITION_DEFINITION = "pdf";

    @Inject
    protected CoreSession session;

    @Inject
    protected RenditionService renditionService;

    @Test
    public void testDummyRendition() throws Exception {
        DocumentModel file = createBlobFile();

        List<Rendition> renditions = renditionService.getAvailableRenditions(file);
        assertEquals(2, renditions.size());

        Rendition dummy = renditionService.getRendition(file, "dummyRendition");
        assertNotNull(dummy);
        assertEquals(dummy.getBlob().getString(), file.getTitle());

        Renderable renderable = file.getAdapter(Renderable.class);
        assertNotNull(renderable);

        assertEquals(2, renderable.getAvailableRenditionDefinitions().size());

    }

    protected DocumentModel createBlobFile() throws ClientException {
        Blob blob = createTextBlob("Dummy text", "dummy.txt");
        DocumentModel file = createFileWithBlob(blob, "dummy-file");
        assertNotNull(file);
        return file;
    }

    protected DocumentModel createFileWithBlob(Blob blob, String name)
            throws ClientException {
        DocumentModel file = session.createDocumentModel("/", name, "File");
        BlobHolder bh = file.getAdapter(BlobHolder.class);
        bh.setBlob(blob);
        file = session.createDocument(file);
        return file;
    }

    protected Blob createTextBlob(String content, String filename) {
        Blob blob = new StringBlob(content, "text/plain");
        blob.setFilename(filename);
        return blob;
    }

}
