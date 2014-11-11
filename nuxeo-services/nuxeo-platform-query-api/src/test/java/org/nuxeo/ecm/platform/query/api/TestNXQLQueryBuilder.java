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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
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

        String query = NXQLQueryBuilder.getQuery(model, whereClause, params,
                sortInfos);
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

        query = NXQLQueryBuilder.getQuery("SELECT * FROM ? WHERE ? = '?'",
                new Object[] { "Document", "dc:title", null }, false, true, null);
        assertEquals("SELECT * FROM Document WHERE dc:title = ''", query);
    }

    @Test
    public void testBuildInQuery() throws Exception {
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

    @Test
    public void testBuildInIntegersQuery() throws Exception {
        PageProviderService pps = Framework.getService(PageProviderService.class);
        WhereClauseDefinition whereClause = pps.getPageProviderDefinition(
                "TEST_IN_INTEGERS").getWhereClause();
        DocumentModel model = new DocumentModelImpl("/", "doc",
                "AdvancedSearch");

        @SuppressWarnings("boxing")
        Integer[] array1 = new Integer[] { 1, 2, 3 };
        model.setPropertyValue("search:integerlist", array1);
        String query = NXQLQueryBuilder.getQuery(model, whereClause, null);
        assertEquals("SELECT * FROM Document WHERE size IN (1, 2, 3)", query);

        @SuppressWarnings("boxing")
        Integer[] array2 = new Integer[] { 1 };
        model.setPropertyValue("search:integerlist", array2);
        query = NXQLQueryBuilder.getQuery(model, whereClause, null);
        assertEquals("SELECT * FROM Document WHERE size = 1", query);

        // criteria with no values are removed
        Integer[] array3 = new Integer[0];
        model.setPropertyValue("search:integerlist", array3);
        query = NXQLQueryBuilder.getQuery(model, whereClause, null);
        assertEquals("SELECT * FROM Document", query);

        // arrays of long work too
        @SuppressWarnings("boxing")
        Long[] array4 = new Long[] { 1L, 2L, 3L };
        model.setPropertyValue("search:integerlist", array4);
        query = NXQLQueryBuilder.getQuery(model, whereClause, null);
        assertEquals("SELECT * FROM Document WHERE size IN (1, 2, 3)", query);

        // lists work too
        @SuppressWarnings("boxing")
        List<Long> list = Arrays.asList(1L, 2L, 3L);
        model.setPropertyValue("search:integerlist", (Serializable) list);
        query = NXQLQueryBuilder.getQuery(model, whereClause, null);
        assertEquals("SELECT * FROM Document WHERE size IN (1, 2, 3)", query);
    }

    @Test
    public void testBuildInIntegersEmptyQuery() throws Exception {
        String pattern = "SELECT * FROM Document WHERE ecm:parentId = ? and ecm:currentLifeCycleState IN (?)";
        Object[] params = new Object[] { "docId", "" };
        String query = NXQLQueryBuilder.getQuery(pattern, params, true, true, null);
        assertEquals(
                "SELECT * FROM Document WHERE ecm:parentId = 'docId' and ecm:currentLifeCycleState IN ('')",
                query);
        params = new Object[] { "docId", new String[] { "foo", "bar" } };
        query = NXQLQueryBuilder.getQuery(pattern, params, true, true, null);
        assertEquals(
                "SELECT * FROM Document WHERE ecm:parentId = 'docId' and ecm:currentLifeCycleState IN ('foo', 'bar')",
                query);
    }

    // @Since 5.9
    @Test
    public void testSortedColumnQuery() throws Exception {
        String pattern = "SELECT * FROM Document WHERE SORTED_COLUMN IS NOT NULL";
        String query = NXQLQueryBuilder.getQuery(pattern, null, true, true, null);
        assertEquals("SELECT * FROM Document WHERE ecm:uuid IS NOT NULL", query);
        SortInfo sortInfo = new SortInfo("dc:title", true);
        query = NXQLQueryBuilder.getQuery(pattern, null, true, true, null, sortInfo);
        assertEquals(
                "SELECT * FROM Document WHERE dc:title IS NOT NULL ORDER BY dc:title",
                query);
        sortInfo = new SortInfo("dc:created", true);
        SortInfo sortInfo2 = new SortInfo("dc:title", true);
        query = NXQLQueryBuilder.getQuery(pattern, null, true, true, null, sortInfo,
                sortInfo2);
        assertEquals(
                "SELECT * FROM Document WHERE dc:created IS NOT NULL ORDER BY dc:created , dc:title",
                query);
    }

    // @Since 5.9.2
    @Test
    public void testCustomSelectStatement() throws Exception {
        PageProviderService pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);
        WhereClauseDefinition whereClause = pps.getPageProviderDefinition(
                "CUSTOM_SELECT_STATEMENT").getWhereClause();
        String[] params = { "foo" };
        DocumentModel model = new DocumentModelImpl("/", "doc",
                "AdvancedSearch");
        model.setPropertyValue("search:title", "bar");

        String query = NXQLQueryBuilder.getQuery(model, whereClause, params);
        assertEquals(
                "SELECT * FROM Note WHERE dc:title LIKE 'bar' AND (ecm:parentId = 'foo')",
                query);
    }

    @Test
    public void iCanFindNamedParameters() {
        //Given a regexp when I put named parameters
        String pattern = "SELECT * FROM Document WHERE ecm:parentId = " +
                ":parentIdVal AND ecm:currentLifeCycleState IN (:param1, " +
                ":param2) AND dc:title = \":pouet\" AND dc:description = " +
                "':desc' AND dc:description = 'ihfifehi:desc'";
        String query = pattern.replaceAll(NXQLQueryBuilder
                .REGEXP_EXCLUDE_DOUBLE_QUOTE, StringUtils.EMPTY);
        query = query.replaceAll(NXQLQueryBuilder
                .REGEXP_EXCLUDE_QUOTE, StringUtils.EMPTY);
        Pattern p1 = Pattern.compile(NXQLQueryBuilder.REGEXP_NAMED_PARAMETER);
        Matcher m1 = p1.matcher(query);

        List<String> matches = new ArrayList<String>();
        // I have to find them
        while (m1.find()) {
            matches.add(m1.group().substring(m1.group().indexOf(":") + 1));
        }
        assertEquals(3, matches.size());
        assertEquals("parentIdVal", matches.get(0));
        assertEquals("param1", matches.get(1));
        assertEquals("param2", matches.get(2));
    }

}
