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
 *     Florent Guillaume
 *     Funsho David
 *
 */
package org.nuxeo.directory.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

/**
 * Test of {@code <autoincrementIdField>true</autoincrementIdField>}.
 */
@RunWith(FeaturesRunner.class)
@Features(DirectoryFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestDirectoryAutoIncrementId {

    private static final String SCHEMA = "intIdSchema";

    @Inject
    protected RuntimeHarness harness;

    @Inject
    protected DirectoryFeature feature;

    @Inject
    protected DirectoryService directoryService;

    @Before
    public void setUp() throws Exception {
        harness.deployContrib(feature.getTestBundleName(), "autoincrementid-contrib.xml");
    }

    @Test
    public void testAutoIncrementId() throws Exception {
        try (Session session = directoryService.open("testAutoIncrement")) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", 42L); // will be ignored
            map.put("label", "foo");
            DocumentModel entry = session.createEntry(map);
            assertNotNull(entry);
            assertEquals(1L, entry.getProperty(SCHEMA, "id"));
            assertEquals("foo", entry.getProperty(SCHEMA, "label"));

            map.clear();
            map.put("label", "bar");
            DocumentModel entry2 = session.createEntry(map);
            assertNotNull(entry2);
            assertEquals(2L, entry2.getProperty(SCHEMA, "id"));
            assertEquals("bar", entry2.getProperty(SCHEMA, "label"));
        }
    }
}
