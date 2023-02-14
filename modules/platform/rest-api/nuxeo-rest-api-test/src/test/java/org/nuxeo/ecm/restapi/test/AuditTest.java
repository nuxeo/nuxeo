/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.common.utils.DateUtils.formatISODateTime;
import static org.nuxeo.ecm.core.io.marshallers.csv.AbstractCSVWriter.TEXT_CSV;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EVENT_ID;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.DateUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.registry.MarshallingConstants;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.io.LogEntryCSVWriter;
import org.nuxeo.ecm.restapi.jaxrs.io.RestConstants;
import org.nuxeo.ecm.restapi.server.jaxrs.adapters.AuditAdapter;
import org.nuxeo.ecm.restapi.server.jaxrs.enrichers.AuditJsonEnricher;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @since 5.7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class, AuditFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class AuditTest extends BaseTest {

    @Inject
    protected AuditLogger auditLogger;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Test
    public void shouldRetrieveAllLogEntriesAsJson() throws Exception {
        DocumentModel doc = RestServerInit.getFile(1, session);

        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME)) {

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(2, nodes.size());
            assertEquals("documentModified", nodes.get(0).get("eventId").asText());
            assertEquals("documentCreated", nodes.get(1).get("eventId").asText());
        }
    }

    @Test
    public void shouldRetrieveAllLogEntriesAsCsv() throws Exception {
        DocumentModel doc = RestServerInit.getFile(1, session);

        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, null, null, null, null, TEXT_CSV)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // Parse the http response to retrieve the csv records
            CSVParser csvParser = new CSVParser(new InputStreamReader(response.getEntityInputStream()),
                    CSVFormat.DEFAULT);
            List<CSVRecord> records = csvParser.getRecords();

            // Csv Header + 2 data lines
            assertEquals(3, records.size());
            int nbeOfColumns = LogEntryCSVWriter.DEFAULT_PROPERTIES.size();
            assertEquals(nbeOfColumns, records.get(0).size());
            assertEquals(nbeOfColumns, records.get(1).size());
            assertEquals(nbeOfColumns, records.get(2).size());

            int index = LogEntryCSVWriter.DEFAULT_PROPERTIES.indexOf(LOG_EVENT_ID);
            assertEquals("eventId", records.get(0).get(index));
            assertEquals("documentModified", records.get(1).get(index));
            assertEquals("documentCreated", records.get(2).get(index));
        }
    }

    @Test
    public void shouldFilterLogEntriesOnEventId() throws Exception {
        DocumentModel doc = RestServerInit.getFile(1, session);

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("eventId", "documentModified");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(1, nodes.size());
            assertEquals("documentModified", nodes.get(0).get("eventId").asText());
        }

        queryParams.putSingle("principalName", "bender");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(0, nodes.size());
        }
    }

    @Test
    public void shouldNotAuditDocumentUpdatedIfNotDirty() throws Exception {
        DocumentModel doc = RestServerInit.getFile(1, session);
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("eventId", "documentModified");
        int nbDocModifiedEvent;
        // Fetch nbDocModifiedEvent
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            nbDocModifiedEvent = nodes.size();
        }
        // Send a PUT on the doc without modification
        JSONDocumentNode jsonDoc;
        try (CloseableClientResponse response = getResponse(RequestType.GET, "id/" + doc.getId())) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            jsonDoc = new JSONDocumentNode((response.getEntityInputStream()));
        }
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "id/" + doc.getId(), jsonDoc.asJson())) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        // Wait for audit indexing
        transactionalFeature.nextTransaction();

        // Check nbDocModifiedEvent is the same
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(nbDocModifiedEvent, nodes.size());
        }
    }

    @Test
    public void shouldFilterLogEntriesOnPrincipalName() throws Exception {
        DocumentModel doc = RestServerInit.getFile(1, session);

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("principalName", "Administrator");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(2, nodes.size());
        }

        queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("principalName", "bender");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(0, nodes.size());
        }
    }

    @Test
    public void shouldFilterLogEntriesOnEventCategories() throws Exception {
        DocumentModel doc = RestServerInit.getFile(1, session);

        List<LogEntry> logEntries = new ArrayList<>();
        logEntries.add(buildLogEntry(doc, "One", "firstEvent", null, null));
        logEntries.add(buildLogEntry(doc, "One", "secondEvent", null, null));
        logEntries.add(buildLogEntry(doc, "Two", "firstEvent", null, null));
        auditLogger.addLogEntries(logEntries);

        transactionalFeature.nextTransaction();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("category", "One");
        queryParams.add("category", "Two");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(3, nodes.size());
        }

        queryParams = new MultivaluedMapImpl();
        queryParams.add("category", "Two");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(1, nodes.size());
        }
    }

    @Test
    public void shouldFilterLogEntriesOnEventDate() throws Exception {
        DocumentModel doc = RestServerInit.getFile(1, session);

        ZonedDateTime firstDate = ZonedDateTime.now();
        ZonedDateTime secondDate = firstDate.plusDays(10);

        List<LogEntry> logEntries = new ArrayList<>();
        logEntries.add(buildLogEntry(doc, "One", "firstEvent", null, firstDate));
        logEntries.add(buildLogEntry(doc, "One", "secondEvent", null, firstDate));
        logEntries.add(buildLogEntry(doc, "One", "firstEvent", null, secondDate));
        auditLogger.addLogEntries(logEntries);

        transactionalFeature.nextTransaction();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("category", "One");
        queryParams.add("startEventDate", formatISODateTime(firstDate.minusDays(1)));
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(3, nodes.size());
        }

        queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("category", "One");
        queryParams.add("startEventDate", formatISODateTime(firstDate.minusDays(1)));
        queryParams.add("endEventDate", formatISODateTime(secondDate.minusDays(1)));
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(2, nodes.size());
        }

        queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("category", "One");
        queryParams.add("startEventDate", formatISODateTime(firstDate.minusDays(1)));
        queryParams.add("endEventDate", formatISODateTime(secondDate.minusDays(1)));
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(2, nodes.size());
        }

        queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("category", "One");
        queryParams.add("startEventDate", formatISODateTime(firstDate.plusDays(1)));
        queryParams.add("endEventDate", formatISODateTime(secondDate.plusDays(1)));
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(1, nodes.size());
        }
    }

    @Test
    public void shouldFilterLogEntriesOnMultipleCriteria() throws Exception {
        DocumentModel doc = RestServerInit.getFile(1, session);

        ZonedDateTime firstDate = ZonedDateTime.now();
        ZonedDateTime secondDate = firstDate.plusDays(10);

        List<LogEntry> logEntries = new ArrayList<>();
        logEntries.add(buildLogEntry(doc, "One", "firstEvent", "bender", firstDate));
        logEntries.add(buildLogEntry(doc, "One", "secondEvent", "leela", firstDate));
        logEntries.add(buildLogEntry(doc, "One", "firstEvent", "leela", secondDate));
        logEntries.add(buildLogEntry(doc, "One", "thirdEvent", "leela", secondDate));
        auditLogger.addLogEntries(logEntries);

        transactionalFeature.nextTransaction();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("category", "One");
        queryParams.add("principalName", "leela");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(3, nodes.size());
        }

        queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("category", "One");
        queryParams.add("principalName", "leela");
        queryParams.add("eventId", "thirdEvent");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(1, nodes.size());
        }

        queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("category", "One");
        queryParams.add("principalName", "leela");
        queryParams.add("eventId", "thirdEvent");
        queryParams.add("startEventDate", formatISODateTime(firstDate.plusDays(1)));
        queryParams.add("endEventDate", formatISODateTime(secondDate.minusNanos(1_000_000)));
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(0, nodes.size());
        }
    }

    @Test
    public void shouldHandlePagination() throws Exception {
        DocumentModel doc = RestServerInit.getFile(1, session);

        List<LogEntry> logEntries = new ArrayList<>();
        logEntries.add(buildLogEntry(doc, "One", "firstEvent", null, null));
        logEntries.add(buildLogEntry(doc, "One", "secondEvent", null, null));
        logEntries.add(buildLogEntry(doc, "One", "thirdEvent", null, null));
        logEntries.add(buildLogEntry(doc, "One", "fourthEvent", null, null));
        logEntries.add(buildLogEntry(doc, "One", "fifthEvent", null, null));
        logEntries.add(buildLogEntry(doc, "One", "sixthEvent", null, null));
        auditLogger.addLogEntries(logEntries);

        transactionalFeature.nextTransaction();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("category", "One");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(6, nodes.size());
        }

        queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("category", "One");
        queryParams.putSingle("currentPageIndex", "0");
        queryParams.putSingle("pageSize", "2");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertTrue(node.get("isPaginable").booleanValue());
            assertEquals(0, node.get("currentPageIndex").intValue());
            assertEquals(2, node.get("pageSize").intValue());
            assertEquals(3, node.get("numberOfPages").intValue());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(2, nodes.size());
            assertEquals("sixthEvent", nodes.get(0).get("eventId").asText());
            assertEquals("fifthEvent", nodes.get(1).get("eventId").asText());
        }

        queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("category", "One");
        queryParams.putSingle("currentPageIndex", "1");
        queryParams.putSingle("pageSize", "3");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertTrue(node.get("isPaginable").booleanValue());
            assertEquals(1, node.get("currentPageIndex").intValue());
            assertEquals(3, node.get("pageSize").intValue());
            assertEquals(2, node.get("numberOfPages").intValue());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(3, nodes.size());
            assertEquals("thirdEvent", nodes.get(0).get("eventId").asText());
            assertEquals("secondEvent", nodes.get(1).get("eventId").asText());
            assertEquals("firstEvent", nodes.get(2).get("eventId").asText());
        }

        queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("category", "One");
        queryParams.putSingle("currentPageIndex", "2");
        queryParams.putSingle("pageSize", "3");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            assertTrue(node.get("isPaginable").booleanValue());
            assertEquals(0, nodes.size());
        }
    }

    /**
     * @since 8.3
     */
    @Test
    public void shouldEnrichWithLatestDocumentLogEntries() throws IOException {
        DocumentModel doc = RestServerInit.getFile(1, session);

        Map<String, String> headers = new HashMap<>();
        headers.put(MarshallingConstants.EMBED_ENRICHERS + ".document", AuditJsonEnricher.NAME);
        try (CloseableClientResponse response = getResponse(RequestType.GET, "id/" + doc.getId(), headers)) {

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            ArrayNode auditNodes = (ArrayNode) node.get(RestConstants.CONTRIBUTOR_CTX_PARAMETERS)
                                                   .get(AuditJsonEnricher.NAME);
            assertEquals(2, auditNodes.size());
            assertEquals("documentModified", auditNodes.get(0).get("eventId").asText());
            assertEquals("documentCreated", auditNodes.get(1).get("eventId").asText());
        }
    }

    @Test
    public void shouldHandleSortingAndPagination() throws Exception {
        DocumentModel doc = RestServerInit.getFile(1, session);

        List<LogEntry> logEntries = new ArrayList<>();
        logEntries.add(buildLogEntry(doc, "Two", "secondEvent", "james", null));
        logEntries.add(buildLogEntry(doc, "Two", "firstEvent", "james", null));
        logEntries.add(buildLogEntry(doc, "Two", "thirdEvent", "james", null));
        logEntries.add(buildLogEntry(doc, "Two", "thirdEvent", "james", null));
        logEntries.add(buildLogEntry(doc, "Two", "thirdEvent", "james", null));
        logEntries.add(buildLogEntry(doc, "Two", "thirdEvent", "james", null));
        logEntries.add(buildLogEntry(doc, "One", "secondEvent", "james", null));
        logEntries.add(buildLogEntry(doc, "One", "thirdEvent", "james", null));
        logEntries.add(buildLogEntry(doc, "One", "firstEvent", "james", null));
        logEntries.add(buildLogEntry(doc, "One", "thirdEvent", "james", null));
        logEntries.add(buildLogEntry(doc, "One", "firstEvent", "james", null));
        auditLogger.addLogEntries(logEntries);

        transactionalFeature.nextTransaction();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("principalName", "james");
        queryParams.putSingle("sortBy", "category,eventId");
        queryParams.putSingle("sortOrder", "asc,desc");
        queryParams.putSingle("pageSize", "5");

        queryParams.putSingle("currentPageIndex", "0");
        makeSortAndPaginationCallAndVerify(doc, queryParams, //
                List.of("One", "One", "One", "One", "One"), //
                List.of("thirdEvent", "thirdEvent", "secondEvent", "firstEvent", "firstEvent"));

        queryParams.putSingle("currentPageIndex", "1");
        makeSortAndPaginationCallAndVerify(doc, queryParams, //
                List.of("Two", "Two", "Two", "Two", "Two"), //
                List.of("thirdEvent", "thirdEvent", "thirdEvent", "thirdEvent", "secondEvent"));

        queryParams.putSingle("currentPageIndex", "2");
        makeSortAndPaginationCallAndVerify(doc, queryParams, //
                List.of("Two"), //
                List.of("firstEvent"));
    }

    protected void makeSortAndPaginationCallAndVerify(DocumentModel doc, MultivaluedMap<String, String> queryParams,
            List<String> expectedCategories, List<String> expectedEvents) throws Exception {
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);

            List<String> categories = nodes.stream().map(n -> n.get("category").asText()).collect(Collectors.toList());
            assertEquals(expectedCategories, categories);

            List<String> events = nodes.stream().map(n -> n.get("eventId").asText()).collect(Collectors.toList());
            assertEquals(expectedEvents, events);
        }
    }

    @Override
    protected List<JsonNode> getLogEntries(JsonNode node) {
        assertEquals("logEntries", node.get("entity-type").asText());
        assertTrue(node.get("entries").isArray());
        List<JsonNode> result = new ArrayList<>();
        Iterator<JsonNode> elements = node.get("entries").elements();
        while (elements.hasNext()) {
            result.add(elements.next());
        }
        return result;
    }

    protected LogEntry buildLogEntry(DocumentModel documentModel, String category, String eventId, String principalName,
            ZonedDateTime eventDate) {
        LogEntry logEntry = auditLogger.newLogEntry();
        logEntry.setDocUUID(documentModel.getRef());
        logEntry.setCategory(category);
        logEntry.setEventId(eventId);
        logEntry.setPrincipalName(principalName);
        logEntry.setEventDate(DateUtils.toDate(eventDate));
        return logEntry;
    }

}
