/*
 * (C) Copyright 2009-2019 Nuxeo (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.publisher.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.helper.PublicationRelationHelper;
import org.nuxeo.ecm.platform.publisher.impl.core.SimpleCorePublishedDocument;
import org.nuxeo.ecm.platform.publisher.test.TestPublicationRelations.Populate;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@RunWith(FeaturesRunner.class)
@Deploy("org.nuxeo.ecm.platform.versioning")
@Deploy("org.nuxeo.ecm.platform.versioning.api")
@RepositoryConfig(init = Populate.class)
public class TestPublicationRelations extends PublisherTestCase {

    public static class Populate implements RepositoryInit {

        @Override
        public void populate(CoreSession session) {
            DocumentModel wsRoot = session.getDocument(new PathRef("/default-domain/workspaces"));

            DocumentModel ws = session.createDocumentModel(wsRoot.getPathAsString(), "ws1", "Workspace");
            ws.setProperty("dublincore", "title", "test WS");
            ws = session.createDocument(ws);

            DocumentModel sectionsRoot = session.getDocument(new PathRef("/default-domain/sections"));

            DocumentModel section1 = session.createDocumentModel(sectionsRoot.getPathAsString(), "section1", "Section");
            section1.setProperty("dublincore", "title", "section1");
            section1 = session.createDocument(section1);

            DocumentModel section2 = session.createDocumentModel(sectionsRoot.getPathAsString(), "section2", "Section");
            section2.setProperty("dublincore", "title", "section2");
            section2 = session.createDocument(section2);

            DocumentModel section11 = session.createDocumentModel(section1.getPathAsString(), "section11", "Section");
            section11.setProperty("dublincore", "title", "section11");
            section11 = session.createDocument(section11);

            DocumentModel doc2Publish = session.createDocumentModel(ws.getPathAsString(), "file", "File");
            doc2Publish.setProperty("dublincore", "title", "MyDoc");

            Blob blob = Blobs.createBlob("SomeDummyContent");
            blob.setFilename("dummyBlob.txt");
            doc2Publish.setProperty("file", "content", blob);

            session.createDocument(doc2Publish);

            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }

    }

    @Inject
    protected PublisherService service;

    @Test
    public void testPublicationRelation() {

        PublicationTree tree = service.getPublicationTree(service.getAvailablePublicationTree().get(0), session, null);
        assertNotNull(tree);

        DocumentModel doc2Publish = session.getDocument(new PathRef("/default-domain/workspaces/ws1/file"));
        List<PublicationNode> nodes = tree.getChildrenNodes();
        PublicationNode targetNode = nodes.get(0);
        PublishedDocument pubDoc = tree.publish(doc2Publish, targetNode);
        assertTrue(pubDoc instanceof SimpleCorePublishedDocument);

        DocumentModel proxy = ((SimpleCorePublishedDocument) pubDoc).getProxy();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        assertTrue(PublicationRelationHelper.isPublished(proxy));

        assertEquals(tree.getConfigName(), service.getPublicationTreeFor(proxy, session).getConfigName());
    }

}
