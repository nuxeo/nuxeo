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

package org.nuxeo.ecm.restapi.test;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.restapi.server.jaxrs.enrichers.HasContentJsonEnricher;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy("org.nuxeo.ecm.platform.restapi.server:OSGI-INF/json-enrichers-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.userworkspace.core")
@Deploy("org.nuxeo.ecm.platform.collections.core")
@Deploy("org.nuxeo.ecm.platform.userworkspace.types")
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.web.common")
public class HasContentJsonEnricherTest extends AbstractJsonWriterTest.Local<DocumentModelJsonWriter, DocumentModel> {

    public HasContentJsonEnricherTest() {
        super(DocumentModelJsonWriter.class, DocumentModel.class);
    }

    @Inject
    private CoreSession session;

    @Before
    public void setup() {
        DocumentModel document = session.createDocumentModel("/", "folder", "Folder");
        document = session.createDocument(document);
        document = session.createDocumentModel("/", "child1", "Folder");
        document = session.createDocument(document);
        document = session.createDocumentModel("/", "child2", "Folder");
        document = session.createDocument(document);
        DocumentModel child11 = session.createDocumentModel("/", "child11", "File");
        child11 = session.createDocument(document);
        DocumentModel collection = session.createDocumentModel("/", "collection1", "Collection");
        collection = session.createDocument(collection);
        collection = session.createDocumentModel("/", "collection2", "Collection");
        collection = session.createDocument(collection);
        CollectionManager colMag = Framework.getService(CollectionManager.class);
        colMag.addToCollection(collection, child11, session);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

    }

    @Test
    public void testFolder() throws Exception {
        DocumentModel root = session.getDocument(new PathRef("/"));
        JsonAssert json = jsonAssert(root, CtxBuilder.enrichDoc(HasContentJsonEnricher.NAME).get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has(HasContentJsonEnricher.NAME).isBool();
        json.isEquals(true);

        DocumentModel child1 = session.getDocument(new PathRef("/child2"));
        json = jsonAssert(child1, CtxBuilder.enrichDoc(HasContentJsonEnricher.NAME).get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has(HasContentJsonEnricher.NAME).isBool();
        json.isEquals(false);
    }

    @Test
    public void testCollection() throws Exception {
        DocumentModel collection = session.getDocument(new PathRef("/collection1"));
        JsonAssert json = jsonAssert(collection, CtxBuilder.enrichDoc(HasContentJsonEnricher.NAME).get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has(HasContentJsonEnricher.NAME).isBool();
        json.isEquals(false);

        collection = session.getDocument(new PathRef("/collection2"));
        json = jsonAssert(collection, CtxBuilder.enrichDoc(HasContentJsonEnricher.NAME).get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has(HasContentJsonEnricher.NAME).isBool();
        json.isEquals(true);
    }

}
