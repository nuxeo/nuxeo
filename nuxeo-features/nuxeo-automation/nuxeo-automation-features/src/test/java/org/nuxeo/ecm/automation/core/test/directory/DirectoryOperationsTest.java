/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 *     Mincong Huang <mhuang@nuxeo.com>
 */

package org.nuxeo.ecm.automation.core.test.directory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.core.operations.services.directory.CreateDirectoryEntries;
import org.nuxeo.ecm.automation.core.operations.services.directory.DeleteDirectoryEntries;
import org.nuxeo.ecm.automation.core.operations.services.directory.ReadDirectoryEntries;
import org.nuxeo.ecm.automation.core.operations.services.directory.SuggestDirectoryEntries;
import org.nuxeo.ecm.automation.core.operations.services.directory.UpdateDirectoryEntries;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, DirectoryFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class)
@Deploy("org.nuxeo.ecm.actions")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.features")
@Deploy("org.nuxeo.ecm.automation.features:test-directories-sql-contrib.xml")
public class DirectoryOperationsTest {

    @Inject
    protected CoreSession session;

    @Inject
    AutomationService service;

    @Inject
    protected DirectoryService directoryService;

    protected void createEntry(String id, String label, int obsolete) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("label", label);
        m.put("obsolete", obsolete);

        try (Session directorySession = directoryService.open("continent")) {
            directorySession.createEntry(m);
        }
    }

    @Test
    public void shouldCreateNewEntriesIfAllParamsFilled() throws Exception {
        String newEntries = "[{\"id\": \"newContinent\", \"label\": \"newLabel\", \"obsolete\": 0},"
                + "{\"id\": \"anotherContinent\", \"label\": \"anotherLabel\", \"obsolete\": 0}]";

        Map<String, Object> params = new HashMap<>();
        params.put("directoryName", "continent");
        params.put("entries", newEntries);
        OperationParameters oparams = new OperationParameters(CreateDirectoryEntries.ID, params);

        OperationContext ctx = new OperationContext(session);
        OperationChain chain = new OperationChain("fakeChain");
        chain.add(oparams);
        Blob result = (Blob) service.run(ctx, chain);
        assertNotNull(result);

        ObjectMapper mapper = new ObjectMapper();
        List<String> createdIds = mapper.readValue(result.getString(), new TypeReference<List<String>>() {
        });

        assertEquals(2, createdIds.size());
        assertEquals("newContinent", createdIds.get(0));
        assertEquals("anotherContinent", createdIds.get(1));

        try (Session directorySession = directoryService.open("continent")) {
            try {
                assertEntriesIn(directorySession);
            } finally {
                // tear down
                createdIds.forEach(directorySession::deleteEntry);
            }
        }
    }

    @Test
    public void shouldCreateNewEntriesIfSomeParamsMissing() throws Exception {
        // Field 'obsolete' is missing on purpose.
        String newEntries = "[{\"id\": \"newContinent\", \"label\": \"newLabel\"},"
                + "{\"id\": \"anotherContinent\", \"label\": \"anotherLabel\"}]";

        Map<String, Object> params = new HashMap<>();
        params.put("directoryName", "continent");
        params.put("entries", newEntries);
        OperationParameters oparams = new OperationParameters(CreateDirectoryEntries.ID, params);

        OperationContext ctx = new OperationContext(session);
        OperationChain chain = new OperationChain("fakeChain");
        chain.add(oparams);
        Blob result = (Blob) service.run(ctx, chain);
        assertNotNull(result);

        ObjectMapper mapper = new ObjectMapper();
        List<String> createdIds = mapper.readValue(result.getString(), new TypeReference<List<String>>() {
        });
        assertEquals(2, createdIds.size());
        assertEquals("newContinent", createdIds.get(0));
        assertEquals("anotherContinent", createdIds.get(1));

        try (Session directorySession = directoryService.open("continent")) {
            try {
                assertEntriesIn(directorySession);
            } finally {
                // tear down
                createdIds.forEach(directorySession::deleteEntry);
            }
        }
    }

    private void assertEntriesIn(Session directorySession) {
        // assert using method 'Session#getEntry'
        DocumentModel entry = directorySession.getEntry("newContinent");
        assertEquals("newLabel", entry.getProperty("vocabulary", "label"));
        assertEquals(0L, entry.getProperty("vocabulary", "obsolete"));

        entry = directorySession.getEntry("anotherContinent");
        assertEquals("anotherLabel", entry.getProperty("vocabulary", "label"));
        assertEquals(0L, entry.getProperty("vocabulary", "obsolete"));

        // assert using method 'Session#query'
        Map<String, Serializable> filter = new HashMap<>();
        filter.put("obsolete", 0L);
        DocumentModelList docs = directorySession.query(filter, Collections.emptySet(), Collections.emptyMap(), false);
        assertEquals(9, docs.size());
    }

    @Test
    public void shouldNotCreateNewEntryIFEntryAlreadyExsists() throws Exception {
        String newEntries = "[{\"id\": \"europe\", \"label\": \"europe\", \"obsolete\": 0}]";

        Map<String, Object> params = new HashMap<>();
        params.put("directoryName", "continent");
        params.put("entries", newEntries);
        OperationParameters oparams = new OperationParameters(CreateDirectoryEntries.ID, params);

        OperationContext ctx = new OperationContext(session);
        OperationChain chain = new OperationChain("fakeChain");
        chain.add(oparams);
        try {
            service.run(ctx, chain);
            fail();
        } catch (DirectoryException e) {
            assertEquals("Failed to invoke operation Directory.CreateEntries, Entry with id europe already exists",
                    e.getMessage());
        }
    }

    @Test
    public void shouldDeleteEntries() throws Exception {
        createEntry("entryToDelete", "entryToDeleteLabel", 0);

        Map<String, Object> params = new HashMap<>();
        params.put("directoryName", "continent");
        params.put("entries", "[\"entryToDelete\"]");
        OperationParameters oparams = new OperationParameters(DeleteDirectoryEntries.ID, params);

        OperationContext ctx = new OperationContext(session);
        OperationChain chain = new OperationChain("fakeChain");
        chain.add(oparams);
        Blob result = (Blob) service.run(ctx, chain);
        assertNotNull(result);

        ObjectMapper mapper = new ObjectMapper();
        List<String> deleteIds = mapper.readValue(result.getString(), new TypeReference<List<String>>() {
        });
        assertEquals(1, deleteIds.size());
        assertEquals("entryToDelete", deleteIds.get(0));

        try (Session directorySession = directoryService.open("continent")) {
            assertNull(directorySession.getEntry("entryToDelete"));
        }
    }

    @Test
    public void shouldMarkEntriesAsObsolete() throws Exception {
        createEntry("entryToMarkAsObsolete", "entryToMarkAsObsoleteLabel", 0);

        Map<String, Object> params = new HashMap<>();
        params.put("directoryName", "continent");
        params.put("markObsolete", true);
        params.put("entries", "[\"entryToMarkAsObsolete\"]");
        OperationParameters oparams = new OperationParameters(DeleteDirectoryEntries.ID, params);

        OperationContext ctx = new OperationContext(session);
        OperationChain chain = new OperationChain("fakeChain");
        chain.add(oparams);
        Blob result = (Blob) service.run(ctx, chain);
        assertNotNull(result);

        ObjectMapper mapper = new ObjectMapper();
        List<String> deleteIds = mapper.readValue(result.getString(), new TypeReference<List<String>>() {
        });
        assertEquals(1, deleteIds.size());
        assertEquals("entryToMarkAsObsolete", deleteIds.get(0));

        try (Session directorySession = directoryService.open("continent")) {
            DocumentModel entry = directorySession.getEntry("entryToMarkAsObsolete");
            assertNotNull(entry);
            assertEquals(1L, entry.getProperty("vocabulary", "obsolete"));
        }
    }

    @Test
    public void shouldUpdateEntries() throws Exception {
        createEntry("entryToUpdate", "entryToUpdateLabel", 0);

        String entriesToUpdate = "[{\"id\": \"entryToUpdate\", \"label\": \"newEntryToUpdateLabel\", \"obsolete\": 0}]";

        Map<String, Object> params = new HashMap<>();
        params.put("directoryName", "continent");
        params.put("entries", entriesToUpdate);
        OperationParameters oparams = new OperationParameters(UpdateDirectoryEntries.ID, params);

        OperationContext ctx = new OperationContext(session);
        OperationChain chain = new OperationChain("fakeChain");
        chain.add(oparams);
        Blob result = (Blob) service.run(ctx, chain);
        assertNotNull(result);

        ObjectMapper mapper = new ObjectMapper();
        List<String> updatedIds = mapper.readValue(result.getString(), new TypeReference<List<String>>() {
        });
        assertEquals(1, updatedIds.size());
        assertEquals("entryToUpdate", updatedIds.get(0));

        try (Session directorySession = directoryService.open("continent")) {
            DocumentModel entry = directorySession.getEntry("entryToUpdate");
            assertNotNull(entry);
            assertEquals("newEntryToUpdateLabel", entry.getProperty("vocabulary", "label"));
        }
    }

    @Test
    public void shouldReadEntries() throws Exception {
        String entriesToRead = "[\"europe\", \"asia\", \"oceania\"]";

        Map<String, Object> params = new HashMap<>();
        params.put("directoryName", "continent");
        params.put("entries", entriesToRead);
        OperationParameters oparams = new OperationParameters(ReadDirectoryEntries.ID, params);

        OperationContext ctx = new OperationContext(session);
        OperationChain chain = new OperationChain("fakeChain");
        chain.add(oparams);
        Blob result = (Blob) service.run(ctx, chain);
        assertNotNull(result);

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> entries = mapper.readValue(result.getString(),
                new TypeReference<List<Map<String, Object>>>() {
                });
        assertEquals(3, entries.size());

        Map<String, Object> entry = entries.get(0);
        assertEquals("europe", entry.get("id"));
        assertEquals("label.directories.continent.europe", entry.get("label"));
        assertEquals(0, entry.get("obsolete"));
        entry = entries.get(1);
        assertEquals("asia", entry.get("id"));
        assertEquals("label.directories.continent.asia", entry.get("label"));
        assertEquals(0, entry.get("obsolete"));
        entry = entries.get(2);
        assertEquals("oceania", entry.get("id"));
        assertEquals("label.directories.continent.oceania", entry.get("label"));
        assertEquals(0, entry.get("obsolete"));
    }

    /**
     * @since 10.1
     */
    @Test
    public void shouldSuggestProperEntries() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("directoryName", "continent");
        OperationParameters oparams = new OperationParameters(SuggestDirectoryEntries.ID, params);

        OperationContext ctx = new OperationContext(session);
        OperationChain chain = new OperationChain("fakeChain");
        chain.add(oparams);
        Blob result = (Blob) service.run(ctx, chain);
        assertNotNull(result);

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> entries = mapper.readValue(result.getString(),
                new TypeReference<List<Map<String, Object>>>() {
                });
        assertEquals(7, entries.size());

        Map<String, Object> entry = entries.get(0);
        assertEquals("africa", entry.get("id"));
        assertEquals(0, entry.get("obsolete"));
        assertEquals("continent", entry.get("directoryName"));
        assertNotNull(entry.get("properties"));
    }

    @Test
    public void shouldFilterSuggestedEntries() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("directoryName", "countries");

        Properties filters = new Properties();
        filters.put("parent","europe");

        params.put("filters",filters);

        OperationParameters oparams = new OperationParameters(SuggestDirectoryEntries.ID, params);

        OperationContext ctx = new OperationContext(session);
        OperationChain chain = new OperationChain("filterChain");
        chain.add(oparams);
        Blob result = (Blob) service.run(ctx, chain);
        assertNotNull(result);

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> entries = mapper.readValue(result.getString(),
                new TypeReference<List<Map<String, Object>>>() {
                });
        assertEquals(1, entries.size());

        List<Object> children = (List<Object>) entries.get(0).get("children");
        assertNotNull(children);
        assertEquals(2, children.size());
    }


}
