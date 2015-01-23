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
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.audio.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/*
 * Tests that the AudioImporter class works by importing a sample audio file
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.types.api", "org.nuxeo.ecm.platform.types.core", "org.nuxeo.ecm.platform.audio.core",
        "org.nuxeo.ecm.platform.filemanager.api", "org.nuxeo.ecm.platform.filemanager.core",
        "org.nuxeo.ecm.platform.mimetype.api", "org.nuxeo.ecm.platform.mimetype.core" })
public class TestAudioImporter {

    protected static final String AUDIO_TYPE = "Audio";

    @Inject
    protected CoreSession session;

    @Inject
    protected FileManager fileManagerService;

    protected DocumentModel root;

    private File getTestFile() {
        return new File(FileUtils.getResourcePathFromContext("test-data/sample.wav"));
    }

    @Before
    public void setUp() throws Exception {
        root = session.getRootDocument();
    }

    @After
    public void tearDown() throws Exception {
        root = null;
    }

    @Test
    public void testAudioType() throws ClientException {

        DocumentType audioType = session.getDocumentType(AUDIO_TYPE);
        assertNotNull("Does our type exist?", audioType);

        // TODO: check get/set properties on common properties of audios

        // Create a new DocumentModel of our type in memory
        DocumentModel docModel = session.createDocumentModel("/", "doc", AUDIO_TYPE);
        assertNotNull(docModel);

        assertNull(docModel.getPropertyValue("common:icon"));
        assertNull(docModel.getPropertyValue("dc:title"));
        assertNull(docModel.getPropertyValue("uid:uid"));
        assertNull(docModel.getPropertyValue("aud:duration"));

        docModel.setPropertyValue("common:icon", "/icons/audio.png");
        docModel.setPropertyValue("dc:title", "testTitle");
        docModel.setPropertyValue("uid:uid", "testUid");
        docModel.setPropertyValue("aud:duration", 133);

        DocumentModel docModelResult = session.createDocument(docModel);
        assertNotNull(docModelResult);

        assertEquals("/icons/audio.png", docModelResult.getPropertyValue("common:icon"));
        assertEquals("testTitle", docModelResult.getPropertyValue("dc:title"));
        assertEquals("testUid", docModelResult.getPropertyValue("uid:uid"));
        assertEquals("133", docModelResult.getPropertyValue("aud:duration").toString());
    }

    @Test
    public void testImportAudio() throws Exception {
        File testFile = getTestFile();
        Blob blob = new FileBlob(testFile, "audio/wav");
        String rootPath = root.getPathAsString();
        assertNotNull(blob);
        assertNotNull(rootPath);
        assertNotNull(session);
        assertNotNull(fileManagerService);

        DocumentModel docModel = fileManagerService.createDocumentFromBlob(session, blob, rootPath, true,
                "test-data/sample.wav");

        assertNotNull(docModel);
        DocumentRef ref = docModel.getRef();
        session.save();

        docModel = session.getDocument(ref);
        assertEquals("Audio", docModel.getType());
        assertEquals("sample.wav", docModel.getTitle());

        Blob contentBlob = (Blob) docModel.getProperty("file", "content");
        assertNotNull(contentBlob);
        assertEquals("sample.wav", docModel.getProperty("file", "filename"));

        // check that we don't get PropertyExceptions when accessing the audio
        // schema

        // TODO: add duration detection
        assertNull(docModel.getPropertyValue("aud:duration"));

        // TODO: add thumbnail generation and picture metadata extraction where
        // they make sense for audios (ie. extract these from the metadata
        // already included in the audio
        // and use them to set the appropriate schema properties)
    }

}
