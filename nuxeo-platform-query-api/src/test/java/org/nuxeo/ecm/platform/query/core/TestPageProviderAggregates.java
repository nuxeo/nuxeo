/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.platform.query.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.AggregateQuery;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.9.6
 */
public class TestPageProviderAggregates extends SQLRepositoryTestCase {

    protected PageProviderService pps;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        openSession();
        pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);
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
        deployContrib("org.nuxeo.ecm.platform.query.api.test",
                "test-schemas-contrib.xml");
    }

    @Test
    public void testAggregateDefinitionEmpty() throws Exception {

        PageProviderDefinition ppd = pps
                .getPageProviderDefinition("CURRENT_DOCUMENT_CHILDREN");
        List<AggregateDefinition> aggs = ppd.getAggregates();
        assertNotNull(aggs);
        assertEquals(0, aggs.size());
        assertEquals(Collections.<AggregateDefinition>emptyList(), aggs);
    }

    @Test
    public void testAggregateDefinition() throws Exception {
        PageProviderDefinition ppd = pps
                .getPageProviderDefinition("TEST_AGGREGATES");
        List<AggregateDefinition> aggs = ppd.getAggregates();
        assertNotNull(aggs);
        assertEquals(5, aggs.size());
        AggregateDefinition source_agg = aggs.get(0);
        AggregateDefinition coverage_agg = aggs.get(1);
        AggregateDefinition subject_agg = aggs.get(2);

        assertEquals("source_agg", source_agg.getId());
        assertEquals("terms", source_agg.getType());
        assertEquals("dc:source", source_agg.getDocumentField());
        assertEquals("advanced_search", source_agg.getSearchField().getSchema());
        assertEquals("source_agg", source_agg.getSearchField().getName());
        assertTrue(source_agg.getPropertiesAsJson().contains("minDocSize"));
        assertTrue(source_agg.getProperties().containsKey("minDocSize"));

        assertEquals("coverage_agg", coverage_agg.getId());
        assertEquals("{\"interval\" : 50}", coverage_agg.getPropertiesAsJson());

        assertEquals("{}", subject_agg.getPropertiesAsJson());

        assertNotNull(source_agg.getProperties());
        assertEquals(2, source_agg.getProperties().size());
        assertEquals(0, subject_agg.getProperties().size());
    }

    @Test
    public void testAggregateQuery() throws Exception {
        PageProviderDefinition ppd = pps
                .getPageProviderDefinition("TEST_AGGREGATES");
        HashMap<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY,
                (AbstractSession) session);
        DocumentModel searchDoc = new DocumentModelImpl("/", "doc",
                "AdvancedSearch");
        String[] query = { "for search", "you know" };
        searchDoc.setPropertyValue("search:source_agg", query);
        PageProvider<?> pp = pps.getPageProvider("TEST_AGGREGATES", ppd,
                searchDoc, null, Long.valueOf(1), Long.valueOf(0), null);
        assertNotNull(pp);

        List<AggregateQuery> qaggs = pp.getAggregatesQuery();
        assertEquals(5, qaggs.size());

        AggregateQuery source_agg = qaggs.get(0);
        assertEquals("AggregateQueryImpl(source_agg, terms, dc:source, [for search, you know])",
                source_agg.toString());

        AggregateQuery coverage_agg = qaggs.get(1);
        assertNotNull(coverage_agg.getSelection());
        assertEquals(0, coverage_agg.getSelection().size());
        assertEquals("AggregateQueryImpl(coverage_agg, histogram, dc:coverage, [])",
                coverage_agg.toString());
    }

}
