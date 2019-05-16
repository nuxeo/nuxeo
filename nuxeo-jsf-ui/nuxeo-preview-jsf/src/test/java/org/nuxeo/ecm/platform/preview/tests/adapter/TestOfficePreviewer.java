/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 *
 */

package org.nuxeo.ecm.platform.preview.tests.adapter;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.convert")
@Deploy("org.nuxeo.ecm.platform.preview")
@Deploy("org.nuxeo.ecm.platform.preview.jsf:OSGI-INF/preview-adapter-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.preview.jsf.tests:doctype-contrib-test.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestOfficePreviewer {

    @Inject
    protected CoreSession session;

    @Inject
    protected MimetypeRegistry mimetypeRegistry;

    @Test
    public void testFilesOfficePreviewer() {
        DocumentModel document = session.createDocumentModel("CustomDoc");
        Blob officeBlob = new FileBlob(FileUtils.getResourceFileFromContext("hello.docx"));
        String mimeType = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault("hello.docx", officeBlob, null);
        officeBlob.setMimeType(mimeType);
        document.setPropertyValue("files:files",
                (Serializable) Collections.singletonList(Collections.singletonMap("file", officeBlob)));

        HtmlPreviewAdapter adapter = document.getAdapter(HtmlPreviewAdapter.class);
        List<Blob> previewBlobs = adapter.getFilePreviewBlobs();
        // check that we have the same result passing the xpath explicitly
        assertEquals(previewBlobs, adapter.getFilePreviewBlobs("files:files/0/file"));
        // check preview blobs
        assertEquals(2, previewBlobs.size());
        assertEquals("text/html", previewBlobs.get(0).getMimeType());
        Blob pdfBlob = previewBlobs.get(1);
        assertEquals("application/pdf", pdfBlob.getMimeType());
        // fix filename set to "pdf" by the PDFPreviewer to ensure blob equality
        pdfBlob.setFilename("hello.pdf");
        Blob expectedBlob = Framework.getService(ConversionService.class)
                                     .convert("any2pdf", new SimpleBlobHolder(officeBlob), null)
                                     .getBlob();
        assertEquals(expectedBlob, pdfBlob);
    }

}
