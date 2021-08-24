/*
 * (C) Copyright 2010-2017 Nuxeo (http://nuxeo.com/) and others.
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

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.api.PageProviderType;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;

/**
 * @author Anahide Tchertchian
 * @since 5.4
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.query.api.test:test-pageprovider-contrib.xml")
public class TestPageProviderService {

    private static final String CURRENT_DOCUMENT_CHILDREN = "CURRENT_DOCUMENT_CHILDREN";

    private static final String FOO = "foo";

    @Inject
    protected HotDeployer deployer;

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected PageProviderService pageProviderService;

    @Test
    public void testRegistration() throws Exception {
        assertNull(pageProviderService.getPageProviderDefinition(FOO));

        PageProviderDefinition def = pageProviderService.getPageProviderDefinition(CURRENT_DOCUMENT_CHILDREN);
        assertNotNull(def);
        assertEquals(CURRENT_DOCUMENT_CHILDREN, def.getName());
        assertNull(def.getWhereClause());
        assertEquals("SELECT * FROM Document WHERE ecm:parentId = ? AND " + "ecm:isVersion = 0 AND ecm:mixinType != "
                + "'HiddenInNavigation' AND ecm:isTrashed = 0", def.getPattern());
        assertEquals(1, def.getSortInfos().size());
        assertEquals("dc:title", def.getSortInfos().get(0).getSortColumn());
        assertTrue(def.getSortInfos().get(0).getSortAscending());
        assertNull(def.getSearchDocumentType());

        def = pageProviderService.getPageProviderDefinition("ADVANCED_SEARCH");
        assertNotNull(def);
        assertEquals("ADVANCED_SEARCH", def.getName());
        assertEquals("ecm:parentId = ?", def.getWhereClause().getFixedPart());
        assertNull(def.getWhereClause().getSelectStatement());
        assertEquals(1, def.getSortInfos().size());
        assertEquals("dc:title", def.getSortInfos().get(0).getSortColumn());
        assertTrue(def.getSortInfos().get(0).getSortAscending());
        assertEquals("AdvancedSearch", def.getSearchDocumentType());

        def = pageProviderService.getPageProviderDefinition("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT");
        assertNotNull(def);
        assertEquals("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT", def.getName());
        assertEquals(
                "ecm:parentId = ? AND ecm:isVersion = 0 AND ecm:mixinType != 'HiddenInNavigation' AND ecm:isTrashed = 0",
                def.getWhereClause().getFixedPart());
        assertEquals("SELECT * FROM Note", def.getWhereClause().getSelectStatement());
        assertEquals(1, def.getSortInfos().size());
        assertEquals("dc:title", def.getSortInfos().get(0).getSortColumn());
        assertTrue(def.getSortInfos().get(0).getSortAscending());
        assertEquals("File", def.getSearchDocumentType());

        // test override
        deployer.deploy("org.nuxeo.ecm.platform.query.api.test:test-pageprovider-override-contrib.xml");
        def = pageProviderService.getPageProviderDefinition("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT");
        assertNotNull(def);
        assertEquals("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT", def.getName());
        assertNull(def.getWhereClause().getFixedPart());
        assertEquals(1, def.getSortInfos().size());
        assertEquals("dc:description", def.getSortInfos().get(0).getSortColumn());
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
        assertNull(pageProviderService.getPageProviderDefinition(FOO));

        PageProviderDefinition def = pageProviderService.getPageProviderDefinition(CURRENT_DOCUMENT_CHILDREN);
        assertNotNull(def);
        assertEquals(CURRENT_DOCUMENT_CHILDREN, def.getName());
        assertEquals(2, def.getPageSize());

        // test override when disabling page provider
        deployer.deploy("org.nuxeo.ecm.platform.query.api.test:test-pageprovider-override-contrib.xml");
        def = pageProviderService.getPageProviderDefinition(CURRENT_DOCUMENT_CHILDREN);
        assertNull(def);

        // test override again after, changed page size
        deployer.deploy("org.nuxeo.ecm.platform.query.api.test:test-pageprovider-override-contrib2.xml");
        def = pageProviderService.getPageProviderDefinition(CURRENT_DOCUMENT_CHILDREN);
        assertEquals(CURRENT_DOCUMENT_CHILDREN, def.getName());
        assertEquals(20, def.getPageSize());
    }

    @Test
    public void testQuery() throws Exception {
        PageProviderDefinition ppd = pageProviderService.getPageProviderDefinition(CURRENT_DOCUMENT_CHILDREN);
        ppd.setPattern("SELECT * FROM Document");
        HashMap<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) coreSession);
        PageProvider<?> pp = pageProviderService.getPageProvider(CURRENT_DOCUMENT_CHILDREN, ppd, null, null,
                Long.valueOf(1), Long.valueOf(0), props);

        assertNotNull(pp);

        List<?> p = pp.getCurrentPage();
        assertNotNull(p);
        assertEquals(0, p.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMergedProperties() throws Exception {
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put("myprop", "foo");
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) pageProviderService.getPageProvider(
                CURRENT_DOCUMENT_CHILDREN, null, null, null, props);
        assertTrue(pp.getProperties().containsKey("myprop"));
        assertTrue(pp.getProperties().containsKey("dummy"));
    }

    @Test
    public void testRegistrationNames() throws Exception {
        Set<String> ppNames = pageProviderService.getPageProviderDefinitionNames();
        assertFalse(ppNames.isEmpty());
        assertTrue(ppNames.contains(CURRENT_DOCUMENT_CHILDREN));
    }

    @Test
    public void testPageSizeOptions() {
        PageProviderDefinition def = pageProviderService.getPageProviderDefinition(CURRENT_DOCUMENT_CHILDREN);
        assertNotNull(def);
        List<Long> options = def.getPageSizeOptions();
        assertEquals(4, options.size());
        assertEquals(2L, options.get(0).longValue());
        assertEquals(10L, options.get(1).longValue());
        assertEquals(15L, options.get(2).longValue());
        assertEquals(20L, options.get(3).longValue());

        def = pageProviderService.getPageProviderDefinition("CURRENT_DOCUMENT_CHILDREN_WITH_SEARCH_DOCUMENT");
        assertNotNull(def);
        options = def.getPageSizeOptions();
        assertEquals(7, options.size());
        assertEquals(2L, options.get(0).longValue());
        assertEquals(5L, options.get(1).longValue());
        assertEquals(10L, options.get(2).longValue());
        assertEquals(20L, options.get(3).longValue());
        assertEquals(30L, options.get(4).longValue());
        assertEquals(40L, options.get(5).longValue());
        assertEquals(50L, options.get(6).longValue());
    }

    /**
     * @since 2021.8
     */
    @Test
    public void testPageProviderType() {
        PageProvider<?> pageProvider = pageProviderService.getPageProvider(CURRENT_DOCUMENT_CHILDREN, null, null, null,
                null);
        assertEquals(PageProviderType.DEFAULT, pageProviderService.getPageProviderType(pageProvider));
    }

}
