/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.filemanager.core.listener.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.filemanager")
@Deploy("org.nuxeo.ecm.platform.filemanager:OSGI-INF/nxfilemanager-digest-contrib.xml")
public class TestDigestComputerListener {

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected CoreSession coreSession;

    protected DocumentModel createFileDocument() {
        DocumentModel fileDoc = coreSession.createDocumentModel("/", "testFile", "File");
        fileDoc.setProperty("dublincore", "title", "TestFile");
        Blob blob = Blobs.createBlob("SOMEDUMMYDATA");
        blob.setFilename("test.pdf");
        blob.setMimeType("application/pdf");
        fileDoc.setProperty("file", "content", blob);
        fileDoc = coreSession.createDocument(fileDoc);
        txFeature.nextTransaction();
        return fileDoc;
    }

    @Test
    public void testDigest() throws Exception {
        DocumentModel file = createFileDocument();
        Blob blob = (Blob) file.getProperty("file", "content");
        assertNotNull(blob);

        String digest = blob.getDigest();
        assertNotNull(digest);
        assertFalse("".equals(digest));
        assertEquals("CJz5xUykO51gRRCIQadZ9dL20NPDd/O0yVBEgP13Skg=", digest);
    }

}
