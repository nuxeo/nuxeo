/*
 * (C) Copyright 2011-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Martins
 *     Florent Guillaume
 *     Marwane Kalam-Alami
 */
package org.nuxeo.ecm.platform.query.api;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.runtime.api.Framework;

public class TestNXQLQueryBuilder extends SQLRepositoryTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.query.api",
                "OSGI-INF/pageprovider-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.query.api.test",
                "test-schemas-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.query.api.test",
                "test-pageprovider-contrib.xml");
    }

    @Test
    public void testBuildIsNullQuery() throws Exception {
        PageProviderService pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);
        WhereClauseDefinition whereClause = pps.getPageProviderDefinition(
                "ADVANCED_SEARCH").getWhereClause();
        SortInfo sortInfos = new SortInfo("dc:title", true);
        String[] params = { "foo" };
        DocumentModel model = new DocumentModelImpl("/", "doc",
                "AdvancedSearch");
        model.setPropertyValue("search:title", "bar");

        String query = NXQLQueryBuilder.getQuery(model, whereClause, params, sortInfos);
        assertEquals(
                "SELECT * FROM Document WHERE dc:title LIKE 'bar' AND (ecm:parentId = 'foo') ORDER BY dc:title",
                query);

        model.setPropertyValue("search:isPresent", Boolean.TRUE);
        query = NXQLQueryBuilder.getQuery(model, whereClause, params, sortInfos);
        assertEquals(
                "SELECT * FROM Document WHERE dc:title LIKE 'bar' AND dc:modified IS NULL AND (ecm:parentId = 'foo') ORDER BY dc:title",
                query);

        // only boolean available in schema without default value
        model.setPropertyValue("search:isPresent", Boolean.FALSE);
        query = NXQLQueryBuilder.getQuery(model, whereClause, params, sortInfos);
        assertEquals(
                "SELECT * FROM Document WHERE dc:title LIKE 'bar' AND dc:modified IS NOT NULL AND (ecm:parentId = 'foo') ORDER BY dc:title",
                query);

    }

    @Test
    public void testBuidInQuery() throws Exception {
        PageProviderService pps = Framework.getService(PageProviderService.class);
        WhereClauseDefinition whereClause = pps.getPageProviderDefinition(
                "TEST_IN").getWhereClause();
        DocumentModel model = new DocumentModelImpl("/", "doc", "File");
        model.setPropertyValue("dc:subjects", new String[] { "foo", "bar" });
        String query = NXQLQueryBuilder.getQuery(model, whereClause, null);
        assertEquals("SELECT * FROM Document WHERE dc:title IN ('foo', 'bar')",
                query);

        model.setPropertyValue("dc:subjects", new String[] { "foo" });
        query = NXQLQueryBuilder.getQuery(model, whereClause, null);
        assertEquals("SELECT * FROM Document WHERE dc:title = 'foo'", query);

        // criteria with no values are removed
        model.setPropertyValue("dc:subjects", new String[] {});
        query = NXQLQueryBuilder.getQuery(model, whereClause, null);
        assertEquals("SELECT * FROM Document", query);
    }

}
