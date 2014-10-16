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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.query.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class TestPageProviderService extends SQLRepositoryTestCase {

    private static final String CURRENT_DOCUMENT_CHILDREN = "CURRENT_DOCUMENT_CHILDREN";

    private static final String FOO = "foo";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        openSession();
    }

    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    @Override
    protected void deployRepositoryContrib() throws Exception {
        super.deployRepositoryContrib();
        deployContrib("org.nuxeo.ecm.platform.query.api",
                "OSGI-INF/pageprovider-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.query.api.test",
                "test-pageprovider-contrib.xml");
    }

    @Test
    public void testRegistration() throws Exception {
        PageProviderService service = Framework.getService(PageProviderService.class);
        assertNotNull(service);

        assertNull(service.getPageProviderDefinition(FOO));

        PageProviderDefinition def = service.getPageProviderDefinition(CURRENT_DOCUMENT_CHILDREN);
        assertNotNull(def);
        assertEquals(CURRENT_DOCUMENT_CHILDREN, def.getName());
        assertNull(def.getWhereClause());
        assertEquals(
                "SELECT * FROM Document WHERE ecm:parentId = ? AND "
                        + "ecm:isCheckedInVersion = 0 AND ecm:mixinType != "
                        + "'HiddenInNavigation' AND ecm:currentLifeCycleState != 'deleted'",
                def.getPattern());
        assertEquals(1, def.getSortInfos().size());
        assertEquals("dc:title", def.getSortInfos().get(0).getSortColumn());
        assertTrue(def.getSortInfos().get(0).getSortAscending());
        assertNull(def.getSearchDocumentType());

        def = service.getPageProviderDefinition("ADVANCED_SEARCH");
        assertNotNull(def);
        assertEquals("ADVANCED_SEARCH", def.getName());
        assertEquals("ecm:parentId = ?", def.getWhereClause().getFixedPart());
        assertNull(def.getWhereClause().getSelectStatement());
        assertEquals(1, def.getSortInfos().size());
        assertEquals("dc:title", def.getSortInfos().get(0).getSortColumn());
        assertTrue(def.getSortInfos().get(0).getSortAscending());
        assertEquals("AdvancedSearch", def.getSearchDocumentType());

        def = service.getPageProviderDefinition("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT");
        assertNotNull(def);
        assertEquals("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT",
                def.getName());
        assertEquals(
                "ecm:parentId = ? AND ecm:isCheckedInVersion = 0 AND ecm:mixinType != 'HiddenInNavigation' AND ecm:currentLifeCycleState != 'deleted'",
                def.getWhereClause().getFixedPart());
        assertEquals("SELECT * FROM Note",
                def.getWhereClause().getSelectStatement());
        assertEquals(1, def.getSortInfos().size());
        assertEquals("dc:title", def.getSortInfos().get(0).getSortColumn());
        assertTrue(def.getSortInfos().get(0).getSortAscending());
        assertEquals("File", def.getSearchDocumentType());

        // test override
        deployContrib("org.nuxeo.ecm.platform.query.api.test",
                "test-pageprovider-override-contrib.xml");

        def = service.getPageProviderDefinition("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT");
        assertNotNull(def);
        assertEquals("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT",
                def.getName());
        assertNull(def.getWhereClause().getFixedPart());
        assertEquals(1, def.getSortInfos().size());
        assertEquals("dc:description",
                def.getSortInfos().get(0).getSortColumn());
        assertFalse(def.getSortInfos().get(0).getSortAscending());
        assertEquals("File2", def.getSearchDocumentType());
    }

    /**
     * non regression test for NXP-9809
     *
     * @since 5.6
     * @throws Exception
     */
    @Test
    public void testRegistrationOverrideEnable() throws Exception {
        PageProviderService service = Framework.getService(PageProviderService.class);
        assertNotNull(service);

        assertNull(service.getPageProviderDefinition(FOO));

        PageProviderDefinition def = service.getPageProviderDefinition(CURRENT_DOCUMENT_CHILDREN);
        assertNotNull(def);
        assertEquals(CURRENT_DOCUMENT_CHILDREN, def.getName());
        assertEquals(2, def.getPageSize());

        // test override when disabling page provider
        deployContrib("org.nuxeo.ecm.platform.query.api.test",
                "test-pageprovider-override-contrib.xml");
        def = service.getPageProviderDefinition(CURRENT_DOCUMENT_CHILDREN);
        assertNull(def);

        // test override again after, changed page size
        deployContrib("org.nuxeo.ecm.platform.query.api.test",
                "test-pageprovider-override-contrib2.xml");
        def = service.getPageProviderDefinition(CURRENT_DOCUMENT_CHILDREN);
        assertEquals(CURRENT_DOCUMENT_CHILDREN, def.getName());
        assertEquals(20, def.getPageSize());
    }

    @Test
    public void testQuery() throws Exception {
        PageProviderService pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);

        PageProviderDefinition ppd = pps.getPageProviderDefinition(CURRENT_DOCUMENT_CHILDREN);
        ppd.setPattern("SELECT * FROM Document");
        HashMap<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY,
                (AbstractSession) session);
        PageProvider<?> pp = pps.getPageProvider(CURRENT_DOCUMENT_CHILDREN,
                ppd, null, null, Long.valueOf(1), Long.valueOf(0), props);

        assertNotNull(pp);

        List<?> p = pp.getCurrentPage();
        assertNotNull(p);
        assertEquals(0, p.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMergedProperties() throws Exception {
        PageProviderService pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put("myprop", "foo");
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) pps.getPageProvider(
                CURRENT_DOCUMENT_CHILDREN, null, null, null, props);
        assertTrue(pp.getProperties().containsKey("myprop"));
        assertTrue(pp.getProperties().containsKey("dummy"));
    }

    @Test
    public void testRegistrationNames() throws Exception {
        PageProviderService service = Framework.getService(PageProviderService.class);
        assertNotNull(service);
        Set<String> ppNames = service.getPageProviderDefinitionNames();
        assertFalse(ppNames.isEmpty());
        assertTrue(ppNames.contains(CURRENT_DOCUMENT_CHILDREN));
    }

}
