/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Policy;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.spi.CmisBinding;
import org.junit.Test;
import org.nuxeo.ecm.core.opencmis.impl.client.NuxeoSession;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoRepository;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;

public class TestNuxeoSession extends SQLRepositoryTestCase {

    private static final String NUXEO_ROOT_TYPE = "Root";

    protected String repositoryId = "test";

    protected NuxeoSession session;

    protected String rootFolderId;

    protected CmisBinding binding;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // deployed for fulltext indexing
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.core.convert.plugins");
        // // MyDocType
        // deployContrib("org.nuxeo.ecm.core.chemistry.tests.test",
        // "OSGI-INF/types-contrib.xml");

        // cmis
        // Map<String, String> params = new HashMap<String, String>();
        // params.put(SessionParameter.BINDING_TYPE,
        // BindingType.CUSTOM.value());
        // params.put(SessionParameter.BINDING_SPI_CLASS,
        // NuxeoSpi.class.getName());
        // params.put(SessionParameter.REPOSITORY_ID, repositoryId);

        // SessionFactory factory = SessionFactoryImpl.newInstance();
        // session = factory.createSession(params);
        openSession();
        NuxeoRepository repository = new NuxeoRepository(repositoryId);
        session = new NuxeoSession(super.session, repository);

        setUpData();
    }

    @Override
    public void tearDown() throws Exception {
        tearDownData();
        super.tearDown();
    }

    protected void setUpData() {
        binding = session.getBinding();
        RepositoryInfo rid = binding.getRepositoryService().getRepositoryInfo(
                repositoryId, null);
        assertNotNull(rid);
        rootFolderId = rid.getRootFolderId();
        assertNotNull(rootFolderId);
    }

    protected void tearDownData() {
        session.close();
    }

    @Test
    public void testRoot() {
        Folder root = session.getRootFolder();
        assertNotNull(root);
        assertNotNull(root.getName());
        assertNotNull(root.getId());
        assertNull(root.getFolderParent());
        assertNotNull(root.getType());
        assertEquals(NUXEO_ROOT_TYPE, root.getType().getId());
        assertEquals(rootFolderId, root.getPropertyValue("cmis:objectId"));
        assertEquals(NUXEO_ROOT_TYPE,
                root.getPropertyValue("cmis:objectTypeId"));
    }

    @Test
    public void testCreateObject() {
        Folder root = session.getRootFolder();
        ContentStream contentStream = null;
        VersioningState versioningState = null;
        List<Policy> policies = null;
        List<Ace> addAces = null;
        List<Ace> removeAces = null;
        OperationContext context = NuxeoSession.DEFAULT_CONTEXT;
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put("cmis:objectTypeId", "Note");
        properties.put("cmis:name", "mynote");
        properties.put("note", "bla bla");
        Document doc = root.createDocument(properties, contentStream,
                versioningState, policies, addAces, removeAces, context);
        assertNotNull(doc.getId());
        assertEquals("mynote", doc.getName());
        assertEquals("mynote", doc.getPropertyValue("dc:title"));
        assertEquals("bla bla", doc.getPropertyValue("note"));
    }

}
