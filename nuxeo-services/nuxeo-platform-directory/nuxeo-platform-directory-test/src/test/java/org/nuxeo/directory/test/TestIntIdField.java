/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */
package org.nuxeo.directory.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 9.2
 */
@RunWith(FeaturesRunner.class)
@Features(DirectoryFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.directory.tests:intIdDirectory-contrib.xml")
public class TestIntIdField {

    protected static final String INT_ID_DIRECTORY = "testIdDirectory";

    @Inject
    protected DirectoryService directoryService;

    @SuppressWarnings("boxing")
    @Test
    public void testIntIdDirectory() {
        try (Session session = directoryService.open(INT_ID_DIRECTORY)) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", 1);
            map.put("label", "toto");
            DocumentModel entry = session.createEntry(map);
            assertNotNull(entry);

            map.put("id", 2);
            map.put("label", "titi");
            DocumentModel entry2 = session.createEntry(map);
            assertNotNull(entry2);

            assertNotNull(session.getEntry("1"));
            assertNotNull(session.getEntry("2"));
            assertNull(session.getEntry("3"));
        }
    }

    /**
     * @since 11.1
     */
    @Test
    public void testQueryBuilderOnIntId() {
        try (Session session = directoryService.open(INT_ID_DIRECTORY)) {
            String key = "label";
            String value = "toto";
            Map<String, Object> map = new HashMap<>();
            map.put("id", 1);
            map.put(key, value);
            session.createEntry(map);

            QueryBuilder queryBuilder = new QueryBuilder().predicate(Predicates.eq(key, value));
            List<String> ids = session.queryIds(queryBuilder);
            assertEquals(1, ids.size());
            assertEquals("1", ids.get(0));
        }
    }

}
