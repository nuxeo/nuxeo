/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.publisher.test;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.impl.core.SimpleCorePublishedDocument;
import org.nuxeo.ecm.platform.publisher.helper.PublicationRelationHelper;
import org.nuxeo.runtime.api.Framework;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.hsqldb.jdbc.jdbcDataSource;

import java.util.List;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class TestPublicationRelations extends AbstractCorePublisherTest {

    public TestPublicationRelations(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        jdbcDataSource ds = new jdbcDataSource();
        ds.setDatabase("jdbc:hsqldb:mem:jena");
        ds.setUser("sa");
        ds.setPassword("");
        Context context = new InitialContext();
        context.bind("java:/nxrelations-default-jena", ds);
        Framework.getProperties().setProperty(
                "org.nuxeo.ecm.sql.jena.databaseType", "HSQL");
        Framework.getProperties().setProperty(
                "org.nuxeo.ecm.sql.jena.databaseTransactionEnabled", "false");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.platform.content.template");
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.relations");
        deployBundle("org.nuxeo.ecm.relations.jena");
        deployContrib("org.nuxeo.ecm.platform.publisher.core",
                "OSGI-INF/publisher-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.publisher.core",
                "OSGI-INF/publisher-contrib.xml");

        openSession();
    }

    public void testPublicationRelation() throws Exception {
        createInitialDocs();

        PublisherService service = Framework.getLocalService(PublisherService.class);
        PublicationTree tree = service.getPublicationTree(
                "DefaultSectionsTree", session, null);
        assertNotNull(tree);

        List<PublicationNode> nodes = tree.getChildrenNodes();
        PublicationNode targetNode = nodes.get(0);
        PublishedDocument pubDoc = tree.publish(doc2Publish, targetNode);
        assertTrue(pubDoc  instanceof SimpleCorePublishedDocument);

        DocumentModel proxy = ((SimpleCorePublishedDocument) pubDoc).getProxy();
        assertTrue(PublicationRelationHelper.isPublished(proxy));

        assertEquals(tree.getConfigName(), PublicationRelationHelper.getTreeNameUsedForPublishing(proxy));
        System.out.println(tree.getConfigName());
    }

}
