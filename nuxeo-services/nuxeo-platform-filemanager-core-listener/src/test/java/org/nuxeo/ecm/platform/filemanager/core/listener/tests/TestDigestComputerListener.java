/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.filemanager.api", //
        "org.nuxeo.ecm.platform.filemanager.core", //
})
@LocalDeploy({
        "org.nuxeo.ecm.platform.filemanager.core.listener:OSGI-INF/filemanager-digestcomputer-event-contrib.xml",
        "org.nuxeo.ecm.platform.filemanager.core.listener.test:OSGI-INF/nxfilemanager-digest-contrib.xml" })
public class TestDigestComputerListener {

    @Inject
    protected CoreSession coreSession;

    protected DocumentModel createFileDocument() throws ClientException {
        DocumentModel fileDoc = coreSession.createDocumentModel("/", "testFile", "File");
        fileDoc.setProperty("dublincore", "title", "TestFile");
        Blob blob = Blobs.createBlob("SOMEDUMMYDATA");
        blob.setFilename("test.pdf");
        blob.setMimeType("application/pdf");
        fileDoc.setProperty("file", "content", blob);
        fileDoc = coreSession.createDocument(fileDoc);
        coreSession.saveDocument(fileDoc);
        coreSession.save();
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
