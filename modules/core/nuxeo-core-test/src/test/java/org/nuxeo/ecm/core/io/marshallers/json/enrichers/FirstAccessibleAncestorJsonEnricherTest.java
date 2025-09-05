/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */

package org.nuxeo.ecm.core.io.marshallers.json.enrichers;

import static org.junit.Assume.assumeTrue;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 10.1
 */
@Features(CoreFeature.class)
@WithFrameworkProperty(name = SQLSession.ALLOW_NEGATIVE_ACL_PROPERTY, value = "true")
@Deploy("org.nuxeo.ecm.core.io:OSGI-INF/doc-type-contrib.xml")
public class FirstAccessibleAncestorJsonEnricherTest
        extends AbstractJsonWriterTest.Local<DocumentModelJsonWriter, DocumentModel> {

    public static final String JOHN = "John";

    public FirstAccessibleAncestorJsonEnricherTest() {
        super(DocumentModelJsonWriter.class, DocumentModel.class);
    }

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Before
    public void setup() {
        DocumentModel document = session.createDocumentModel("/", "child1", "MyFolder");
        session.createDocument(document);
        document = session.createDocumentModel("/child1", "child11", "MyFolder");
        session.createDocument(document);
        document = session.createDocumentModel("/child1/child11", "child111", "MyFolder");
        session.createDocument(document);
    }

    @Test
    public void iCanSeeParentWithAdmin() throws Exception {
        JsonAssert json = jsonAssert(session.getDocument(new PathRef("/child1/child11")),
                CtxBuilder.enrichDoc(FirstAccessibleAncestorJsonEnricher.NAME).get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has(FirstAccessibleAncestorJsonEnricher.NAME).isObject();
        json = json.has("path");
        json.isEquals("/child1");
    }

    @Test
    public void iCanSeeParentWithNonAdmin() throws Exception {
        addReadPermission("/");
        CoreSession nonAdminSession = CoreInstance.getCoreSession(session.getRepositoryName(), JOHN);
        JsonAssert json = jsonAssert(nonAdminSession.getDocument(new PathRef("/child1/child11")),
                CtxBuilder.session(nonAdminSession).enrichDoc(FirstAccessibleAncestorJsonEnricher.NAME).get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has(FirstAccessibleAncestorJsonEnricher.NAME).isObject();
        json = json.has("path");
        json.isEquals("/child1");
    }

    @Test
    public void iCanSeeRootAncestorWithNonAdmin() throws Exception {
        assumeTrue("DBS does not support negative ACLs", !coreFeature.getStorageConfiguration().isDBS());
        addReadPermission("/");
        denyReadPermission("/child1");
        addReadPermission("/child1/child11/child111");
        CoreSession nonAdminSession = CoreInstance.getCoreSession(session.getRepositoryName(), JOHN);
        JsonAssert json = jsonAssert(nonAdminSession.getDocument(new PathRef("/child1/child11/child111")),
                CtxBuilder.session(nonAdminSession).enrichDoc(FirstAccessibleAncestorJsonEnricher.NAME).get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has(FirstAccessibleAncestorJsonEnricher.NAME).isObject();
        json = json.has("path");
        json.isEquals("/");
    }

    @Test
    public void iCannotSeeAncestorWithNonAdmin() throws Exception {
        addReadPermission("/child1/child11");
        CoreSession nonAdminSession = CoreInstance.getCoreSession(session.getRepositoryName(), JOHN);
        JsonAssert json = jsonAssert(nonAdminSession.getDocument(new PathRef("/child1/child11")),
                CtxBuilder.session(nonAdminSession).enrichDoc(FirstAccessibleAncestorJsonEnricher.NAME).get());
        json = json.has("contextParameters").isObject();
        json.properties(0);
    }

    @Test
    public void iCanSeeGrandParentWithNonAdmin() throws Exception {
        assumeTrue("DBS does not support negative ACLs", !coreFeature.getStorageConfiguration().isDBS());
        denyReadPermission("/");
        addReadPermission("/child1");
        denyReadPermission("/child1/child11");
        addReadPermission("/child1/child11/child111");
        CoreSession nonAdminSession = CoreInstance.getCoreSession(session.getRepositoryName(), JOHN);
        JsonAssert json = jsonAssert(nonAdminSession.getDocument(new PathRef("/child1/child11/child111")),
                CtxBuilder.session(nonAdminSession).enrichDoc(FirstAccessibleAncestorJsonEnricher.NAME).get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has(FirstAccessibleAncestorJsonEnricher.NAME).isObject();
        json = json.has("path");
        json.isEquals("/child1");
    }

    @Test
    public void iCanSeeRootHasNoAncestor() throws Exception {
        JsonAssert json = jsonAssert(session.getDocument(new PathRef("/")),
                CtxBuilder.enrichDoc(FirstAccessibleAncestorJsonEnricher.NAME).get());
        json = json.has("contextParameters").isObject();
        json.properties(0);
    }

    protected void addReadPermission(String path) {
        var documentModel = session.getDocument(new PathRef(path));
        ACP acp = documentModel.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE(JOHN, SecurityConstants.READ, true));
        session.setACP(documentModel.getRef(), acp, false);
        session.save();
    }

    protected void denyReadPermission(String path) {
        var documentModel = session.getDocument(new PathRef(path));
        ACP acp = documentModel.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE(JOHN, SecurityConstants.READ, false));
        session.setACP(documentModel.getRef(), acp, false);
        session.save();
    }

}
