/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Arnaud Kervern <akervern@nuxeo.com>
 */

package org.nuxeo.ecm.directory.sql;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.sql.filter.SQLBetweenFilter;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(SQLDirectoryFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.directory.sql.tests:pagingDirectory-contrib.xml")
public class TestPagingComplexFilterDirectory {

    private static final String DIR = "pagingDirectory";

    @Inject
    protected DirectoryService directoryService;

    @Test
    public void testPaging() {
        try (Session session = directoryService.open(DIR)) {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put("label", "Label");

            Map<String, String> order = new HashMap<String, String>();
            order.put("id", "ASC");

            List<DocumentModel> entries = session.query(filter, filter.keySet());
            assertEquals(12, entries.size());
            assertEquals("1", entries.get(0).getId());

            entries = session.query(filter, filter.keySet(), order, false, 5, -1);
            assertEquals(5, entries.size());
            assertEquals("1", entries.get(0).getId());

            entries = session.query(filter, filter.keySet(), order, false, 5, 1);
            assertEquals(5, entries.size());
            assertEquals("2", entries.get(0).getId());

            entries = session.query(filter, filter.keySet(), order, false, 5, 11);
            assertEquals(1, entries.size());
            assertEquals("12", entries.get(0).getId());
        }
    }

    @Test
    public void testComplexFilter() {
        try (Session session = directoryService.open(DIR)) {
            Calendar d121110 = new DateTime(2012, 11, 10, 0, 0, 0, 0).toGregorianCalendar();
            Calendar d121211 = new DateTime(2012, 12, 11, 0, 0, 0, 0).toGregorianCalendar();
            Calendar d121224 = new DateTime(2012, 12, 24, 0, 0, 0, 0).toGregorianCalendar();

            SQLBetweenFilter betweenFilter = new SQLBetweenFilter(d121110, d121224);
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put("date", betweenFilter);
            List<DocumentModel> entries = session.query(filter);
            assertEquals(12, entries.size());

            betweenFilter = new SQLBetweenFilter(d121211, d121224);
            filter.put("date", betweenFilter);
            entries = session.query(filter);
            assertEquals(2, entries.size());

            filter.put("type", "something");
            entries = session.query(filter);
            assertEquals(1, entries.size());
        }
    }
}
