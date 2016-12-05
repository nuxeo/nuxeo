/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.automation.core.test.directory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.core.operations.services.directory.CreateDirectoryEntries;
import org.nuxeo.ecm.automation.core.operations.services.directory.DeleteDirectoryEntries;
import org.nuxeo.ecm.automation.core.operations.services.directory.ReadDirectoryEntries;
import org.nuxeo.ecm.automation.core.operations.services.directory.UpdateDirectoryEntries;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class)
@Deploy({ "org.nuxeo.ecm.directory.api", "org.nuxeo.ecm.directory", "org.nuxeo.ecm.directory.sql",
        "org.nuxeo.ecm.directory.types.contrib", "org.nuxeo.ecm.actions", "org.nuxeo.ecm.automation.core",
        "org.nuxeo.ecm.automation.features" })
@LocalDeploy("org.nuxeo.ecm.automation.features:test-directories-sql-contrib.xml")
public class DirectoryOperationsTest {

    @Inject
    protected CoreSession session;

    @Inject
    AutomationService service;

    @Inject
    protected DirectoryService directoryService;

    protected void createEntry(String id, String label, int obsolete) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("id", id);
        m.put("label", label);
        m.put("obsolete", obsolete);

        try (Session directorySession = directoryService.open("continent")) {
            directorySession.createEntry(m);
        }
    }

    @Test
    public void shouldCreateNewEntries() throws Exception {
        String newEntries = "[{\"id\": \"newContinent\"," + "\"label\": \"newLabel\"," + "\"obsolete\": 0},"
                + "{\"id\": \"anotherContinent\"," + "\"label\": \"anotherLabel\"," + "\"obsolete\": 0}]";

        Map<String, Object> params = new HashMap<String, Object>();
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
            DocumentModel entry = directorySession.getEntry("newContinent");
            assertNotNull(entry);
            assertEquals("newLabel", entry.getProperty("vocabulary", "label"));
            entry = directorySession.getEntry("anotherContinent");
            assertNotNull(entry);
            assertEquals("anotherLabel", entry.getProperty("vocabulary", "label"));
        }
    }

    @Test
    public void shouldNotCreateNewEntryIFEntryAlreadyExsists() throws Exception {
        String newEntries = "[{\"id\": \"europe\"," + "\"label\": \"europe\"," + "\"obsolete\": 0}]";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("directoryName", "continent");
        params.put("entries", newEntries);
        OperationParameters oparams = new OperationParameters(CreateDirectoryEntries.ID, params);

        OperationContext ctx = new OperationContext(session);
        OperationChain chain = new OperationChain("fakeChain");
        chain.add(oparams);
        try {
            service.run(ctx, chain);
            fail();
        } catch (OperationException e) {
            if (!(e.getCause() instanceof DirectoryException)) {
                fail();
            }
            assertEquals(e.getCause().getMessage(), "Entry with id europe already exists");
        }

    }

    @Test
    public void shouldDeleteEntries() throws Exception {
        createEntry("entryToDelete", "entryToDeleteLabel", 0);

        Map<String, Object> params = new HashMap<String, Object>();
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

        Map<String, Object> params = new HashMap<String, Object>();
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

        String entriesToUpdate = "[{\"id\": \"entryToUpdate\"," + "\"label\": \"newEntryToUpdateLabel\","
                + "\"obsolete\": 0}]";

        Map<String, Object> params = new HashMap<String, Object>();
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

        Map<String, Object> params = new HashMap<String, Object>();
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

}
