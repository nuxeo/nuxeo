/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.platform.query.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;

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
        AggregateDefinition[] aggs = ppd.getAggregates();
        assertNotNull(aggs);
        assertEquals(0, aggs.length);
    }

    @Test
    public void testAggregateDefinition() throws Exception {
        PageProviderDefinition ppd = pps
                .getPageProviderDefinition("TEST_AGGREGATES");
        AggregateDefinition[] aggs = ppd.getAggregates();
        assertNotNull(aggs);
        assertEquals(3, aggs.length);
        assertEquals("source_agg", aggs[0].getId());
        assertEquals("terms", aggs[0].getType());
        assertEquals("dc:source", aggs[0].getDocumentField());
        assertEquals("advanced_search", aggs[0].getSearchField().getSchema());
        assertEquals("source_agg", aggs[0].getSearchField().getName());
        assertTrue(aggs[0].getPropertiesAsJson().contains("size"));
        assertTrue(aggs[0].getProperties().containsKey("size"));

        assertEquals("{\"interval\" : 50}", aggs[1].getPropertiesAsJson());
        assertEquals("{}", aggs[2].getPropertiesAsJson());

        assertNotNull(aggs[0].getProperties());
        assertEquals(2, aggs[0].getProperties().size());
        assertEquals(0, aggs[2].getProperties().size());
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

        AggregateQuery[] qaggs = pp.getAggregatesQuery();
        assertEquals(3, qaggs.length);

        assertEquals("source_agg", qaggs[0].getId());
        assertEquals("terms", qaggs[0].getType());

        assertNotNull(qaggs[0].getSelection());
        assertEquals(2, qaggs[0].getSelection().length);
        assertEquals("for search", qaggs[0].getSelection()[0]);

        assertNotNull(qaggs[1].getSelection());
        assertEquals(0, qaggs[1].getSelection().length);

    }

}
