/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.audio.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.filemanager.api.FileImporterContext;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/*
 * Tests that the AudioImporter class works by importing a sample audio file
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.types")
@Deploy("org.nuxeo.ecm.platform.audio.core")
@Deploy("org.nuxeo.ecm.platform.filemanager")
@Deploy("org.nuxeo.ecm.platform.rendition.core")
@Deploy("org.nuxeo.ecm.platform.tag")
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
    public void testAudioType() {

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
        Blob blob = Blobs.createBlob(testFile, "audio/wav");
        String rootPath = root.getPathAsString();
        assertNotNull(blob);
        assertNotNull(rootPath);
        assertNotNull(session);
        assertNotNull(fileManagerService);

        FileImporterContext context = FileImporterContext.builder(session, blob, rootPath)
                                                         .overwrite(true)
                                                         .build();
        DocumentModel docModel = fileManagerService.createOrUpdateDocument(context);

        assertNotNull(docModel);
        DocumentRef ref = docModel.getRef();
        session.save();

        docModel = session.getDocument(ref);
        assertEquals("Audio", docModel.getType());
        assertEquals("sample.wav", docModel.getTitle());

        Blob contentBlob = (Blob) docModel.getProperty("file", "content");
        assertNotNull(contentBlob);
        assertEquals("sample.wav", contentBlob.getFilename());

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
