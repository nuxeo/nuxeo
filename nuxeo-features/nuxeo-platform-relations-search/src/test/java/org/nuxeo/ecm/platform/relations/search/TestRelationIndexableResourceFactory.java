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
 * $Id$
 */

package org.nuxeo.ecm.platform.relations.search;

import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedData;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResource;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResource;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
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
import org.nuxeo.ecm.platform.relations.services.RelationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 *
 */
public class TestRelationIndexableResourceFactory extends NXRuntimeTestCase {

    private RelationIndexableResourceFactory factory;

    private SearchService searchService;

    private RelationService relationService;

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

        factory = new RelationIndexableResourceFactory();

        // Local lookup is enough
        searchService = Framework.getLocalService(SearchService.class);
        assertNotNull(searchService);
        relationService = (RelationService) Framework.getRuntime()
            .getComponent(RelationService.NAME);
        assertNotNull(relationService);

        resourceConf = searchService.getIndexableResourceConfByName(
                "comments", false);
    }

    public static Object getResolvedValue(ResolvedResource r, String fieldName) {
        return r.getIndexableDataByName(fieldName).getValue();
    }

    public void testNoConfPureURIs() throws Exception {
        Subject subject = NodeFactory.createResource("test://subj");
        Node object = NodeFactory.createResource("test://obj");
        Resource predicate = NodeFactory.createResource("test://dependsOn");
        Statement statement = new StatementImpl(subject, predicate, object);

        IndexableResource iResource = factory.createIndexableResourceFrom(
                statement, null, null);

        assertEquals(statement,
                ((RelationIndexableResourceImpl)iResource).getStatement());

        ResolvedResource resolved = factory.resolveResourceFor(iResource);
        assertEquals("test://subj",
                getResolvedValue(resolved, BuiltinRelationsFields.SUBJECT_URI));
        assertEquals("test://obj",
                getResolvedValue(resolved, BuiltinRelationsFields.OBJECT_URI));
        assertEquals("test://dependsOn",
                getResolvedValue(resolved, BuiltinRelationsFields.PREDICATE_URI));
    }

    public void testConfiguredPureURIs() throws Exception {
        Subject subject = NodeFactory.createResource("test://subj");
        Node object = NodeFactory.createResource("test://obj");
        Resource predicate = NodeFactory.createResource("test://dependsOn");
        Statement statement = new StatementImpl(subject, predicate, object);

        IndexableResource iResource = factory.createIndexableResourceFrom(
                statement, resourceConf, null);

        assertEquals(statement,
                ((RelationIndexableResourceImpl)iResource).getStatement());

        ResolvedResource resolved = factory.resolveResourceFor(iResource);
        assertEquals("test://subj",
                getResolvedValue(resolved, BuiltinRelationsFields.SUBJECT_URI));
        assertEquals("test://obj",
                getResolvedValue(resolved, BuiltinRelationsFields.OBJECT_URI));
        assertEquals("test://dependsOn",
                getResolvedValue(resolved, BuiltinRelationsFields.PREDICATE_URI));
    }

    public void testProperty() throws Exception {
        Subject subject = NodeFactory.createResource("test://subj");
        Node object = NodeFactory.createResource("test://obj");
        Resource predicate = NodeFactory.createResource("test://dependsOn");
        Statement statement = new StatementImpl(subject, predicate, object);

        QNameResource property = NodeFactory.createQNameResource(
                Constants.PROPERTIES_NAMESPACE, "foo");
        LiteralImpl value = NodeFactory.createLiteral("yes");
        statement.setProperty(property, value);
        IndexableResource iResource
            = factory.createIndexableResourceFrom(statement, resourceConf, null);

        ResolvedResource resolved = factory.resolveResourceFor(iResource);
        assertEquals(resourceConf, resolved.getConfiguration());
        assertEquals("yes", getResolvedValue(resolved, "foo"));
        ResolvedData foo =  resolved.getIndexableDataByName("foo");
        // cf XML contribution
        assertTrue(foo.isIndexed());
        assertFalse(foo.isStored());
        assertEquals("foo", foo.getName());
        assertEquals("Text", foo.getTypeName());
        assertEquals("anal", foo.getAnalyzerName());
        assertFalse(foo.isMultiple());
    }

}
