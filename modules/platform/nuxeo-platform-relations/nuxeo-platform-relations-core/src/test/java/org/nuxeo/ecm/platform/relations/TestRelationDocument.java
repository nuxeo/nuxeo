/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.platform.relations;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests a Relation {@link DocumentModel}.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.relations")
@Deploy("org.nuxeo.ecm.platform.dublincore")
@Deploy("org.nuxeo.ecm.relations:relation-core-test-contrib.xml")
public class TestRelationDocument {

    @Inject
    protected CoreSession session;

    @Inject
    protected RelationManager relationManager;

    protected DocumentModel file1;

    @Before
    public void init() {

        file1 = session.createDocument(session.createDocumentModel("/", "file1", "File"));
    }

    @Test
    public void testRelationDocument() {

        // Add a text relation to file1
        QNameResource docResource = (QNameResource) relationManager.getResource(RelationConstants.DOCUMENT_NAMESPACE,
                file1, null);
        Resource predicate = new ResourceImpl("http://purl.org/dc/terms/Requires");
        Node object = new LiteralImpl("The related text");
        Statement statement = new StatementImpl(docResource, predicate, object);
        statement.addProperty(RelationConstants.COMMENT, new LiteralImpl("The relation comment"));
        Graph graph = relationManager.getGraph("myrelations", session);
        graph.add(statement);
        session.save();

        // Check file1 relation document
        DocumentModelList docRelations = session.query(String.format(
                "select * from Relation where relation:source = '%s'", file1.getId()));
        assertEquals(1, docRelations.size());
        DocumentModel relation = docRelations.get(0);
        assertEquals("http://purl.org/dc/terms/Requires", relation.getPropertyValue("relation:predicate"));
        assertEquals("The related text", relation.getPropertyValue("relation:targetString"));
        assertEquals("Administrator", relation.getPropertyValue("dc:creator"));
        assertEquals("The relation comment", relation.getPropertyValue("dc:description"));
    }

}
