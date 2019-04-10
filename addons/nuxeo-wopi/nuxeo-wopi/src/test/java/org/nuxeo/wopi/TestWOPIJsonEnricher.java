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

import static org.nuxeo.wopi.Constants.FILE_CONTENT_PROPERTY;

import java.io.Serializable;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
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
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.wopi.lock.LockHelper;

/**
 * Tests the {@link WOPIJsonEnricher}.
 *
 * @since 10.3
 */
@Features(PlatformFeature.class)
@Deploy("org.nuxeo.ecm.platform.web.common")
@Deploy("org.nuxeo.wopi")
public class TestWOPIJsonEnricher extends AbstractJsonWriterTest.Local<DocumentModelJsonWriter, DocumentModel> {

    public TestWOPIJsonEnricher() {
        super(DocumentModelJsonWriter.class, DocumentModel.class);
    }

    @Inject
    private CoreSession session;

    @Inject
    protected UserManager userManager;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Test
    public void testWOPIJsonEnricher() throws Exception {

        // create test doc
        DocumentModel doc = session.createDocumentModel("/", "wopiDoc", "File");
        doc = session.createDocument(doc);

        // grant Read access to joe on the root document
        DocumentModel rootDocument = session.getRootDocument();
        setPermission(rootDocument, "joe", SecurityConstants.READ);

        // no blob
        JsonAssert json = jsonAssert(doc, CtxBuilder.enrichDoc(WOPIJsonEnricher.NAME).get());
        json = json.has("contextParameters").isObject();
        json.properties(0);

        // blob with an extension not supported by WOPI
        Blob blob = Blobs.createBlob("dummy content");
        blob.setFilename("content.txt");
        json = jsonAssert(doc, CtxBuilder.enrichDoc(WOPIJsonEnricher.NAME).get());
        json = json.has("contextParameters").isObject();
        json.properties(0);

        // blob with an extension supported by WOPI
        blob.setFilename("content.docx");
        doc.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) blob);
        session.saveDocument(doc);
        try (CloseableCoreSession joeSession = coreFeature.openCoreSession("joe")) {
            // read access only, expecting view action URL only
            // document not locked
            doc = joeSession.getDocument(doc.getRef());
            json = jsonAssert(doc, CtxBuilder.enrichDoc(WOPIJsonEnricher.NAME).get());
            json = json.has("contextParameters").isObject();
            json.properties(1);
            json = json.has(WOPIJsonEnricher.NAME).isObject();
            json.has("view")
                .isEquals(RenderingContext.DEFAULT_URL + "wopi/view/" + doc.getRepositoryName() + "/" + doc.getId());
            json.hasNot("edit");
            json.has("locked").isBool().isEquals(false);
            json.has("appName").isText().isEquals("Word");

            // write access, expecting view and edit action URLs
            // lock the document, expecting locked to be true
            setPermission(rootDocument, "joe", SecurityConstants.READ_WRITE);
            String fileId = FileInfo.computeFileId(doc, FILE_CONTENT_PROPERTY);
            LockHelper.addLock(fileId, "wopiLock");
            json = jsonAssert(doc, CtxBuilder.enrichDoc(WOPIJsonEnricher.NAME).get());
            json = json.has("contextParameters").isObject();
            json.properties(1);
            json = json.has(WOPIJsonEnricher.NAME).isObject();
            json.has("view")
                .isEquals(RenderingContext.DEFAULT_URL + "wopi/view/" + doc.getRepositoryName() + "/" + doc.getId());
            json.has("edit")
                .isEquals(RenderingContext.DEFAULT_URL + "wopi/edit/" + doc.getRepositoryName() + "/" + doc.getId());
            json.has("locked").isBool().isEquals(true);
            json.has("appName").isText().isEquals("Word");
        }

    }

    protected void setPermission(DocumentModel doc, String user, String permission) {
        ACP acp = doc.getACP();
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        localACL.add(new ACE(user, permission, true));
        doc.setACP(acp, true);
    }

}
