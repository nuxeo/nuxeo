/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <grenard@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json.enrichers;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 8.10
 */
@LocalDeploy("org.nuxeo.ecm.core.io:OSGI-INF/doc-type-contrib.xml")
public class HasFolderishChildJsonEnricherTest extends AbstractJsonWriterTest.Local<DocumentModelJsonWriter, DocumentModel> {

    public HasFolderishChildJsonEnricherTest() {
        super(DocumentModelJsonWriter.class, DocumentModel.class);
    }

    @Inject
    private CoreSession session;

    @Before
    public void setup() {
        DocumentModel document = session.createDocumentModel("/", "child1", "MyFolder");
        document = session.createDocument(document);
        document = session.createDocumentModel("/", "child2", "MyFolder");
        document = session.createDocument(document);
        document = session.createDocumentModel("/", "child3", "CSDoc");
        document = session.createDocument(document);
    }

    @Test
    public void test() throws Exception {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        DocumentModel root = session.getDocument(new PathRef("/"));
        JsonAssert json = jsonAssert(root, CtxBuilder.enrichDoc(HasFolderishChildJsonEnricher.NAME).get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has(HasFolderishChildJsonEnricher.NAME).isBool();
        json.isEquals(true);

        DocumentModel child1 = session.getDocument(new PathRef("/child1"));
        json = jsonAssert(child1, CtxBuilder.enrichDoc(HasFolderishChildJsonEnricher.NAME).get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has(HasFolderishChildJsonEnricher.NAME).isBool();
        json.isEquals(false);

        DocumentModel child3 = session.getDocument(new PathRef("/child3"));
        json = jsonAssert(child3, CtxBuilder.enrichDoc(HasFolderishChildJsonEnricher.NAME).get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has(HasFolderishChildJsonEnricher.NAME).isBool();
        json.isEquals(false);
    }

}
