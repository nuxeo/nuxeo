/*
 * (C) Copyright 2013-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.audit.service.AuditComponent.DEFAULT_AUDIT_BACKEND;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.api.Route;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.audit.service.AuditRouter;
import org.nuxeo.audit.test.AuditFeature;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.services.AuditPageProviderOperation;
import org.nuxeo.ecm.automation.core.util.Paginable;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.automation.features.AutomationFeaturesFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features({ AuditFeature.class, AutomationFeaturesFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD, init = TestDocumentAuditPageProviderOperation.Populate.class)
public class TestDocumentAuditPageProviderOperation {

    private static final int MAX_ENTRIES = 500;

    /**
     * The number of {@link LogEntry log entries} that has been generated during the repository initialization:
     * <ul>
     * <li>1 {@code documentCreated} for {@code /section}</li>
     * <li>1 {@code documentCreated} for {@code /doc}</li>
     * <li>5 {@code documentUpdated} for {@code /doc}</li>
     * <li>1 {@code documentCheckedIn} for {@code /doc}</li>
     * <li>1 {@code documentCreated} for version 0.1 of {@code /doc}</li>
     * <li>5 {@code documentUpdated} for {@code /doc}</li>
     * <li>1 {@code documentCheckedIn} for {@code /doc} in the publish context</li>
     * <li>1 {@code documentCreated} for version 0.2 of {@code /doc} in the publish context</li>
     * <li>1 {@code documentCreated} for {@code /section/doc} in the publish context (proxy creation)</li>
     * <li>1 {@code documentProxyPublished} for {@code /section/doc} in the publish context</li>
     * <li>1 {@code sectionContentPublished} for version 0.2 of {@code /section} in the publish context</li>
     * <li>5 {@code documentUpdated} for {@code /doc}</li>
     * <li>1 {@code someEvent} for a manually added log entry</li>
     * </ul>
     */
    protected static final long NB_LOG_ENTRIES_AFTER_REPOSITORY_INIT = 25;

    /**
     * wait at least 1s to be sure we have a precise timestamp in all DB backend.
     */
    protected static void sleep() {
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NuxeoException(e);
        }
    }

    public static class Populate implements RepositoryInit {

        @Override
        public void populate(CoreSession session) {
            DocumentModel section = session.createDocumentModel("/", "section", "Folder");
            section = session.createDocument(section);

            DocumentModel doc = session.createDocumentModel("/", "doc", "File");
            doc.setPropertyValue("dc:title", "TestDoc");

            // create the doc
            doc = session.createDocument(doc);

            // do some updates
            for (int i = 0; i < 5; i++) {
                doc.setPropertyValue("dc:description", "Update " + i);
                doc.putContextData("comment", "Update " + i);
                doc = session.saveDocument(doc);
            }

            sleep();

            // create a version
            doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
            doc = session.saveDocument(doc);

            sleep();

            // do some more updates
            for (int i = 5; i < 10; i++) {
                doc.setPropertyValue("dc:description", "Update " + i);
                doc.putContextData("comment", "Update " + i);
                doc = session.saveDocument(doc);
            }

            sleep();

            session.publishDocument(doc, section);

            sleep();

            // do some more updates
            for (int i = 10; i < 15; i++) {
                doc.setPropertyValue("dc:description", "Update " + i);
                doc.putContextData("comment", "Update " + i);
                doc = session.saveDocument(doc);
            }

            List<LogEntry> newEntries = new ArrayList<>();

            LogEntry entry = LogEntry.builder("someEvent", new Date())
                                     .category("somecat")
                                     .principalName("toto")
                                     .build();

            newEntries.add(entry);
            Framework.getService(AuditRouter.class)
                     .routeToBackends(newEntries, List.of(Route.allEventsTo(DEFAULT_AUDIT_BACKEND)));
        }

    }

    @Inject
    protected AuditBackend backend;

    @Inject
    protected AutomationService service;

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature txFeature;

    protected DocumentModel doc;

    protected DocumentModel proxy;

    protected List<DocumentModel> versions;

    protected OperationContext ctx;

    @Before
    public void initOperationContext() {
        ctx = new OperationContext(session);
    }

    @After
    public void closeOperationContext() {
        ctx.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSimpleQuery() throws Exception {

        Map<String, Object> params = new HashMap<>();
        params.put("query", "SELECT * FROM LogEntry");
        params.put("pageSize", MAX_ENTRIES);
        params.put("maxResults", MAX_ENTRIES);
        params.put("currentPageIndex", 0);

        var entries = (List<LogEntry>) service.run(ctx, AuditPageProviderOperation.ID, params);
        assertEquals(NB_LOG_ENTRIES_AFTER_REPOSITORY_INIT, entries.size());
        params.put("pageSize", 5);
        entries = (List<LogEntry>) service.run(ctx, AuditPageProviderOperation.ID, params);
        assertEquals(5, entries.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOwnerQuery() throws Exception {

        Map<String, Object> params = new HashMap<>();
        params.put("query", "SELECT * FROM LogEntry WHERE principalName=?");
        params.put("pageSize", MAX_ENTRIES);
        params.put("maxResults", MAX_ENTRIES);
        params.put("currentPageIndex", 0);

        StringList queryParams = new StringList();
        queryParams.add("$currentUser");
        params.put("queryParams", queryParams);

        var entries = (List<LogEntry>) service.run(ctx, AuditPageProviderOperation.ID, params);
        assertFalse(entries.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEmptyPageProviderQuery() throws Exception {

        Map<String, Object> params = new HashMap<>();
        params.put("providerName", "AUDIT_BROWSER");
        params.put("maxResults", MAX_ENTRIES);
        params.put("pageSize", 10);
        params.put("currentPageIndex", 0);

        var entries = (Paginable<LogEntry>) service.run(ctx, AuditPageProviderOperation.ID, params);

        assertEquals(10, entries.size());
        assertEquals(NB_LOG_ENTRIES_AFTER_REPOSITORY_INIT, entries.getResultsCount());
        assertTrue(entries.getNumberOfPages() > 1);

        int total = entries.size();
        for (int i = 1; i < entries.getNumberOfPages(); i++) {
            params.put("currentPageIndex", i);
            entries = (Paginable<LogEntry>) service.run(ctx, AuditPageProviderOperation.ID, params);
            total += entries.size();
        }
        assertEquals(NB_LOG_ENTRIES_AFTER_REPOSITORY_INIT, total);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNonEmptyPageProviderQuery() throws Exception {

        Map<String, Object> params = new HashMap<>();
        params.put("providerName", "AUDIT_BROWSER");
        params.put("pageSize", MAX_ENTRIES);
        params.put("maxResults", MAX_ENTRIES);
        params.put("currentPageIndex", 0);

        Properties namedParams = new Properties();
        namedParams.put("bas:eventIds", "sectionContentPublished,someEvent");
        params.put("namedQueryParams", namedParams);

        var entries = (List<LogEntry>) service.run(ctx, AuditPageProviderOperation.ID, params);
        assertEquals(2, entries.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNonEmptyPageProviderQuery2() throws Exception {

        Map<String, Object> params = new HashMap<>();
        params.put("providerName", "AUDIT_BROWSER");
        params.put("pageSize", MAX_ENTRIES);
        params.put("maxResults", MAX_ENTRIES);
        params.put("currentPageIndex", 0);

        Properties namedParams = new Properties();
        namedParams.put("bas:eventIds", "sectionContentPublished,someEvent");
        namedParams.put("bas:principalNames", "toto");

        params.put("namedQueryParams", namedParams);

        var entries = (List<LogEntry>) service.run(ctx, AuditPageProviderOperation.ID, params);
        assertEquals(1, entries.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPageProviderQueryViaId() throws Exception {

        Map<String, Object> params = new HashMap<>();
        params.put("providerName", "AUDIT_BROWSER");
        params.put("pageSize", 10);
        params.put("maxResults", 10);
        params.put("currentPageIndex", 0);

        var entries = (List<LogEntry>) service.run(ctx, AuditPageProviderOperation.ID, params);

        long lastId = entries.getLast().getId();

        Properties namedParams = new Properties();
        namedParams.put("bas:logId", "" + lastId);
        params.put("namedQueryParams", namedParams);

        entries = (List<LogEntry>) service.run(ctx, AuditPageProviderOperation.ID, params);

        assertEquals(lastId + 1, entries.getFirst().getId());
    }

}
