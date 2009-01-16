/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id:TestSearchEnginePluginRegistration.java 13121 2007-03-01 18:07:58Z janguenot $
 */

package org.nuxeo.ecm.platform.relations.search;

import java.util.Calendar;

import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.factory.IndexableResourceFactory;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.ResourceTypeDescriptor;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.Subject;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.NodeFactory;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.search.resources.indexing.RelationIndexableResourceFactory;
import org.nuxeo.ecm.platform.relations.search.resources.indexing.RelationIndexableResourceImpl;
import org.nuxeo.ecm.platform.relations.search.resources.indexing.api.BuiltinRelationsFields;
import org.nuxeo.ecm.platform.relations.search.resources.indexing.api.RelationIndexableResource;
import org.nuxeo.ecm.platform.relations.search.resources.indexing.api.ResourceType;
import org.nuxeo.ecm.platform.relations.services.RelationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * Test search engine plugins registration.
 *
 * @author <a href="mailto:gr@nuxeo.com">Georges Racinet</a>
 */
public class TestRelationIndexableResource extends NXRuntimeTestCase {

    private SearchService searchService;

    private IndexableResourceConf resourceConf;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployContrib("org.nuxeo.ecm.platform.relations.search.tests",
                "nxsearch-relations-test-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.relations.search.tests",
                "nxsearch-relations-test-contrib.xml");
        // all-in-one for relations
        deployContrib("org.nuxeo.ecm.platform.relations.search.tests",
                "nxrelations-test-search-bundle.xml");

        // Local lookup is enough
        searchService = Framework.getLocalService(SearchService.class);
        assertNotNull(searchService);
        RelationService relationService = (RelationService) Framework.getRuntime()
                .getComponent(RelationService.NAME);
        assertNotNull(relationService);

        resourceConf = searchService.getIndexableResourceConfByName(
                "comments", false);
    }

    public void testRegistration() {
        ResourceTypeDescriptor desc = searchService
            .getResourceTypeDescriptorByName(ResourceType.RELATIONS);
        assertNotNull(desc);
        IndexableResourceFactory factory = desc.getFactory();
        assertTrue(factory instanceof RelationIndexableResourceFactory);
        assertTrue(factory.createEmptyIndexableResource() instanceof
                RelationIndexableResource);
    }

    public void testComputeId() {
        Subject subject = NodeFactory.createResource("test://subj");
        Node object = NodeFactory.createResource("test://obj");
        Node object2 = NodeFactory.createResource("test://other");
        Resource predicate = NodeFactory.createResource("test://dependsOn");
        Statement statement1 = new StatementImpl(subject, predicate, object);
        Statement statement2 = new StatementImpl(subject, predicate, object2);

        RelationIndexableResourceImpl iResource1 =
            new RelationIndexableResourceImpl(statement1);
        RelationIndexableResourceImpl iResource2 =
            new RelationIndexableResourceImpl(statement2);
        assertNotSame(iResource1.computeId(), iResource2.computeId());
    }

    public void testPureURIs() throws Exception {
        Subject subject = NodeFactory.createResource("test://subj");
        Node object = NodeFactory.createResource("test://obj");
        Resource predicate = NodeFactory.createResource("test://dependsOn");
        Statement statement = new StatementImpl(subject, predicate, object);

        RelationIndexableResourceImpl iResource =
            new RelationIndexableResourceImpl(statement);
        assertEquals("test://subj", iResource.getValueFor(
                BuiltinRelationsFields.SUBJECT_URI));
        assertEquals("test://obj", iResource.getValueFor(
                BuiltinRelationsFields.OBJECT_URI));
        assertEquals("test://dependsOn", iResource.getValueFor(
                BuiltinRelationsFields.PREDICATE_URI));
    }

    /**
     * Tests document representation and qualified name resources
     * namespaces & local name.
     *
     * @throws Exception
     */
    public void testDocumentRepresentation() throws Exception {
        Resource doc = NodeFactory.createQNameResource(
                Constants.DOCUMENT_NAMESPACE, "the_id");
        Resource other = NodeFactory.createResource("test://obj");
        Resource predicate = NodeFactory.createResource("test://dependsOn");

        // Subject
        RelationIndexableResourceImpl iResource = new RelationIndexableResourceImpl(
                new StatementImpl(doc, predicate, other));
        assertEquals("unittest:the_id", iResource.getValueFor(
                BuiltinRelationsFields.SUBJECT));
        assertEquals("the_id", iResource.getValueFor(
                BuiltinRelationsFields.SUBJECT_URI_LOCAL));
        assertEquals(Constants.DOCUMENT_NAMESPACE,
                iResource.getValueFor(
                        BuiltinRelationsFields.SUBJECT_URI_NAMESPACE));
        // Object
        iResource = new RelationIndexableResourceImpl(
                        new StatementImpl(other, predicate, doc));
        assertEquals("unittest:the_id", iResource.getValueFor(
                BuiltinRelationsFields.OBJECT));
        assertEquals("the_id", iResource.getValueFor(
                BuiltinRelationsFields.OBJECT_URI_LOCAL));
        assertEquals(Constants.DOCUMENT_NAMESPACE,
                iResource.getValueFor(
                        BuiltinRelationsFields.OBJECT_URI_NAMESPACE));
    }

    public void testSimpleProperty() throws Exception {
        Subject subject = NodeFactory.createResource("test://subj");
        Node object = NodeFactory.createResource("test://obj");
        Resource predicate = NodeFactory.createResource("test://dependsOn");
        Statement statement = new StatementImpl(subject, predicate, object);

        QNameResource property = NodeFactory.createQNameResource(
                Constants.PROPERTIES_NAMESPACE, "foo");
        LiteralImpl value = NodeFactory.createLiteral("yes");
        statement.setProperty(property, value);
        RelationIndexableResourceImpl iResource =
            new RelationIndexableResourceImpl(statement, resourceConf);
        assertEquals("yes", iResource.getValueFor("foo"));
    }

    public void testIntProperty() throws Exception {
        Subject subject = NodeFactory.createResource("test://subj");
        Node object = NodeFactory.createResource("test://obj");
        Resource predicate = NodeFactory.createResource("test://dependsOn");
        Statement statement = new StatementImpl(subject, predicate, object);

        QNameResource property = NodeFactory.createQNameResource(
                Constants.PROPERTIES_NAMESPACE, "cred");
        LiteralImpl value = NodeFactory.createLiteral("2");
        statement.setProperty(property, value);
        RelationIndexableResourceImpl iResource =
            new RelationIndexableResourceImpl(statement, resourceConf);
        assertEquals(2, iResource.getValueFor("cred"));
    }

    public void testDateProperty() throws Exception {
        Subject subject = NodeFactory.createResource("test://subj");
        Node object = NodeFactory.createResource("test://obj");
        Resource predicate = NodeFactory.createResource("test://dependsOn");
        Statement statement = new StatementImpl(subject, predicate, object);

        QNameResource property = NodeFactory.createQNameResource(
                Constants.PROPERTIES_NAMESPACE, "severeDate");
        LiteralImpl value = NodeFactory.createLiteral("2012-11-23T03:57:28");
        statement.setProperty(property, value);
        RelationIndexableResourceImpl iResource =
            new RelationIndexableResourceImpl(statement, resourceConf);

        Calendar calExpected = Calendar.getInstance();
        calExpected.clear();
        calExpected.set(2012, 10, 23, 3, 57, 28);

        Calendar calOutput = (Calendar)
            iResource.getValueFor("severeDate");
        assertNotNull(calOutput);
        assertEquals(calExpected.getTime(), calOutput.getTime());
    }

}
