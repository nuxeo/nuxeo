/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.wopi;

import static org.nuxeo.ecm.core.api.Blobs.createBlob;
import static org.nuxeo.wopi.JSONHelper.readFile;
import static org.nuxeo.wopi.TestConstants.DOC_ID_VAR;
import static org.nuxeo.wopi.TestConstants.FILE_CONTENT_PROPERTY;
import static org.nuxeo.wopi.TestConstants.REPOSITORY_VAR;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.wopi.lock.LockHelper;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Tests the {@link WOPIJsonEnricher}.
 *
 * @since 10.3
 */
@Features(WOPIFeature.class)
public class TestWOPIJsonEnricher extends AbstractJsonWriterTest.Local<DocumentModelJsonWriter, DocumentModel> {

    public TestWOPIJsonEnricher() {
        super(DocumentModelJsonWriter.class, DocumentModel.class);
    }

    @Inject
    private CoreSession session;

    @Inject
    protected CoreFeature coreFeature;

    @Test
    public void testSimpleWOPIJsonEnricher() throws Exception {
        // create test doc
        DocumentModel doc = session.createDocumentModel("/", "wopiDoc", "File");
        doc = session.createDocument(doc);

        // grant Read access to joe on the root document
        DocumentModel rootDocument = session.getRootDocument();
        setPermission(rootDocument, SecurityConstants.READ);

        // no blob
        JsonAssert json = jsonAssert(doc, CtxBuilder.enrichDoc(WOPIJsonEnricher.NAME).get());
        json = json.has("contextParameters").isObject();
        json.properties(0);

        // blob with an unsupported extension
        Blob blob = createBlob("dummy content");
        blob.setFilename("content.txt");
        doc.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) blob);
        session.saveDocument(doc);
        json = jsonAssert(doc, CtxBuilder.enrichDoc(WOPIJsonEnricher.NAME).get());
        json = json.has("contextParameters").isObject();
        json.properties(0);

        // blob with a supported extension
        blob.setFilename("content.docx");
        doc.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) blob);
        session.saveDocument(doc);
        Map<String, String> toReplace = new HashMap<>();
        toReplace.put(REPOSITORY_VAR, doc.getRepositoryName());
        toReplace.put(DOC_ID_VAR, doc.getId());

        try (CloseableCoreSession joeSession = coreFeature.openCoreSession("joe")) {
            // document not locked
            doc = joeSession.getDocument(doc.getRef());
            json = jsonAssert(doc, CtxBuilder.enrichDoc(WOPIJsonEnricher.NAME).get());

            File file = FileUtils.getResourceFileFromContext("json/testWOPIJsonEnricher-unlocked.json");
            String expected = readFile(file, toReplace);
            JSONAssert.assertEquals(expected, json.toString(), false);

            // add lock, expecting locked to be true
            String fileId = FileInfo.computeFileId(doc, FILE_CONTENT_PROPERTY);
            LockHelper.addLock(fileId, "wopiLock");
            json = jsonAssert(doc, CtxBuilder.enrichDoc(WOPIJsonEnricher.NAME).get());

            file = FileUtils.getResourceFileFromContext("json/testWOPIJsonEnricher-locked.json");
            expected = readFile(file, toReplace);
            JSONAssert.assertEquals(expected, json.toString(), false);
        }
    }

    @Test
    public void testMultipleBlobsWOPIJsonEnricher() throws Exception {
        // create test doc with 4 blobs
        DocumentModel doc = session.createDocumentModel("/", "wopiDoc", "File");
        Blob blob = createBlob("dummy content", null, null, "content.rtf");
        doc.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) blob);
        List<Blob> blobs = Arrays.asList(createBlob("one", null, null, "one.docx"),
                createBlob("two", null, null, "two.bin"), createBlob("three", null, null, "three.xlsx"));
        List<Map<String, Serializable>> files = blobs.stream()
                                                     .map(b -> Collections.singletonMap("file", (Serializable) b))
                                                     .collect(Collectors.toList());
        doc.setPropertyValue("files:files", (Serializable) files);
        doc = session.createDocument(doc);

        // grant Write access to joe on the root document
        DocumentModel rootDocument = session.getRootDocument();
        setPermission(rootDocument, SecurityConstants.READ_WRITE);

        Map<String, String> toReplace = new HashMap<>();
        toReplace.put(REPOSITORY_VAR, doc.getRepositoryName());
        toReplace.put(DOC_ID_VAR, doc.getId());

        try (CloseableCoreSession joeSession = coreFeature.openCoreSession("joe")) {
            // document not locked
            doc = joeSession.getDocument(doc.getRef());
            JsonAssert json = jsonAssert(doc, CtxBuilder.enrichDoc(WOPIJsonEnricher.NAME).get());

            File file = FileUtils.getResourceFileFromContext("json/testMultipleBlobsWOPIJsonEnricher.json");
            String expected = readFile(file, toReplace);
            JSONAssert.assertEquals(expected, json.toString(), false);
        }
    }

    protected void setPermission(DocumentModel doc, String permission) {
        ACP acp = doc.getACP();
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        localACL.add(new ACE("joe", permission, true));
        doc.setACP(acp, true);
    }

}
