/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.registry.MarshallingConstants;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.restapi.jaxrs.io.RestConstants;
import org.nuxeo.ecm.restapi.server.jaxrs.adapters.AuditAdapter;
import org.nuxeo.ecm.restapi.server.jaxrs.enrichers.AuditJsonEnricher;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @since 5.7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class, AuditFeature.class })
@ServletContainer(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class AuditTest extends BaseTest {

    @Inject
    AuditLogger auditLogger;

    @Test
    public void shouldRetrieveAllLogEntries() throws Exception {
        DocumentModel doc = RestServerInit.getFile(1, session);

        try (CloseableClientResponse response = getResponse(BaseTest.RequestType.GET,
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
    public void shouldFilterLogEntriesOnEventId() throws Exception {
        DocumentModel doc = RestServerInit.getFile(1, session);

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("eventId", "documentModified");
        try (CloseableClientResponse response = getResponse(BaseTest.RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(1, nodes.size());
            assertEquals("documentModified", nodes.get(0).get("eventId").asText());
        }

        queryParams.putSingle("principalName", "bender");
        try (CloseableClientResponse response = getResponse(BaseTest.RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(0, nodes.size());
        }
    }

    @Test
    public void shouldFilterLogEntriesOnPrincipalName() throws Exception {
        DocumentModel doc = RestServerInit.getFile(1, session);

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("principalName", "Administrator");
        try (CloseableClientResponse response = getResponse(BaseTest.RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(2, nodes.size());
        }

        queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("principalName", "bender");
        try (CloseableClientResponse response = getResponse(BaseTest.RequestType.GET,
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
        LogEntry logEntry = auditLogger.newLogEntry();
        logEntry.setDocUUID(doc.getRef());
        logEntry.setCategory("One");
        logEntry.setEventId("firstEvent");
        logEntries.add(logEntry);
        logEntry = auditLogger.newLogEntry();
        logEntry.setDocUUID(doc.getRef());
        logEntry.setCategory("One");
        logEntry.setEventId("secondEvent");
        logEntries.add(logEntry);
        logEntry = auditLogger.newLogEntry();
        logEntry.setDocUUID(doc.getRef());
        logEntry.setCategory("Two");
        logEntry.setEventId("firstEvent");
        logEntries.add(logEntry);
        auditLogger.addLogEntries(logEntries);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("category", "One");
        queryParams.add("category", "Two");
        try (CloseableClientResponse response = getResponse(BaseTest.RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(3, nodes.size());
        }

        queryParams = new MultivaluedMapImpl();
        queryParams.add("category", "Two");
        try (CloseableClientResponse response = getResponse(BaseTest.RequestType.GET,
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

        DateTime firstDate = new DateTime();
        DateTime secondDate = firstDate.plusDays(10);

        List<LogEntry> logEntries = new ArrayList<>();
        LogEntry logEntry = auditLogger.newLogEntry();
        logEntry.setDocUUID(doc.getRef());
        logEntry.setCategory("One");
        logEntry.setEventId("firstEvent");
        logEntry.setEventDate(firstDate.toDate());
        logEntries.add(logEntry);
        logEntry = auditLogger.newLogEntry();
        logEntry.setDocUUID(doc.getRef());
        logEntry.setCategory("One");
        logEntry.setEventId("secondEvent");
        logEntry.setEventDate(firstDate.toDate());
        logEntries.add(logEntry);
        logEntry = auditLogger.newLogEntry();
        logEntry.setDocUUID(doc.getRef());
        logEntry.setCategory("One");
        logEntry.setEventId("firstEvent");
        logEntry.setEventDate(secondDate.toDate());
        logEntries.add(logEntry);
        auditLogger.addLogEntries(logEntries);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("category", "One");
        queryParams.add("startEventDate", ISODateTimeFormat.date().print(firstDate.minusDays(1)));
        try (CloseableClientResponse response = getResponse(BaseTest.RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(3, nodes.size());
        }

        queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("category", "One");
        queryParams.add("startEventDate", ISODateTimeFormat.date().print(firstDate.minusDays(1)));
        queryParams.add("endEventDate", ISODateTimeFormat.date().print(secondDate.minusDays(1)));
        try (CloseableClientResponse response = getResponse(BaseTest.RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(2, nodes.size());
        }

        queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("category", "One");
        queryParams.add("startEventDate", DateParser.formatW3CDateTime(firstDate.minusDays(1).toDate()));
        queryParams.add("endEventDate", DateParser.formatW3CDateTime(secondDate.minusDays(1).toDate()));
        try (CloseableClientResponse response = getResponse(BaseTest.RequestType.GET,
                "id/" + doc.getId() + "/@" + AuditAdapter.NAME, queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> nodes = getLogEntries(node);
            assertEquals(2, nodes.size());
        }

        queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("category", "One");
        queryParams.add("startEventDate", ISODateTimeFormat.date().print(firstDate.plusDays(1)));
        queryParams.add("endEventDate", ISODateTimeFormat.date().print(secondDate.plusDays(1)));
        try (CloseableClientResponse response = getResponse(BaseTest.RequestType.GET,
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

        DateTime firstDate = new DateTime();
        DateTime secondDate = firstDate.plusDays(10);

        List<LogEntry> logEntries = new ArrayList<>();
        LogEntry logEntry = auditLogger.newLogEntry();
        logEntry.setDocUUID(doc.getRef());
        logEntry.setCategory("One");
        logEntry.setEventId("firstEvent");
        logEntry.setPrincipalName("bender");
        logEntry.setEventDate(firstDate.toDate());
        logEntries.add(logEntry);
        logEntry = auditLogger.newLogEntry();
        logEntry.setDocUUID(doc.getRef());
        logEntry.setCategory("One");
        logEntry.setEventId("secondEvent");
        logEntry.setPrincipalName("leela");
        logEntry.setEventDate(firstDate.toDate());
        logEntries.add(logEntry);
        logEntry = auditLogger.newLogEntry();
        logEntry.setDocUUID(doc.getRef());
        logEntry.setCategory("One");
        logEntry.setEventId("firstEvent");
        logEntry.setPrincipalName("leela");
        logEntry.setEventDate(secondDate.toDate());
        logEntries.add(logEntry);
        logEntry = auditLogger.newLogEntry();
        logEntry.setDocUUID(doc.getRef());
        logEntry.setCategory("One");
        logEntry.setEventId("thirdEvent");
        logEntry.setPrincipalName("leela");
        logEntry.setEventDate(secondDate.toDate());
        logEntries.add(logEntry);
        auditLogger.addLogEntries(logEntries);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("category", "One");
        queryParams.add("principalName", "leela");
        try (CloseableClientResponse response = getResponse(BaseTest.RequestType.GET,
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
        try (CloseableClientResponse response = getResponse(BaseTest.RequestType.GET,
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
        queryParams.add("startEventDate", ISODateTimeFormat.date().print(firstDate.plusDays(1)));
        queryParams.add("endEventDate", ISODateTimeFormat.date().print(secondDate.minus(1)));
        try (CloseableClientResponse response = getResponse(BaseTest.RequestType.GET,
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
        LogEntry logEntry = auditLogger.newLogEntry();
        logEntry.setDocUUID(doc.getRef());
        logEntry.setCategory("One");
        logEntry.setEventId("firstEvent");
        logEntries.add(logEntry);
        logEntry = auditLogger.newLogEntry();
        logEntry.setDocUUID(doc.getRef());
        logEntry.setCategory("One");
        logEntry.setEventId("secondEvent");
        logEntries.add(logEntry);
        logEntry = auditLogger.newLogEntry();
        logEntry.setDocUUID(doc.getRef());
        logEntry.setCategory("One");
        logEntry.setEventId("thirdEvent");
        logEntries.add(logEntry);
        logEntry = auditLogger.newLogEntry();
        logEntry.setDocUUID(doc.getRef());
        logEntry.setCategory("One");
        logEntry.setEventId("fourthEvent");
        logEntries.add(logEntry);
        logEntry = auditLogger.newLogEntry();
        logEntry.setDocUUID(doc.getRef());
        logEntry.setCategory("One");
        logEntry.setEventId("fifthEvent");
        logEntries.add(logEntry);
        logEntry = auditLogger.newLogEntry();
        logEntry.setDocUUID(doc.getRef());
        logEntry.setCategory("One");
        logEntry.setEventId("sixthEvent");
        logEntries.add(logEntry);
        auditLogger.addLogEntries(logEntries);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("category", "One");
        try (CloseableClientResponse response = getResponse(BaseTest.RequestType.GET,
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
        try (CloseableClientResponse response = getResponse(BaseTest.RequestType.GET,
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
        try (CloseableClientResponse response = getResponse(BaseTest.RequestType.GET,
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
        try (CloseableClientResponse response = getResponse(BaseTest.RequestType.GET,
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
    public void shouldEnrichWithLatestDocumentLogEntries() throws JsonProcessingException, IOException {
        DocumentModel doc = RestServerInit.getFile(1, session);

        Map<String, String> headers = new HashMap<>();
        headers.put(MarshallingConstants.EMBED_ENRICHERS + ".document", AuditJsonEnricher.NAME);
        try (CloseableClientResponse response = getResponse(BaseTest.RequestType.GET, "id/" + doc.getId(), headers)) {

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            ArrayNode auditNodes = (ArrayNode) node.get(RestConstants.CONTRIBUTOR_CTX_PARAMETERS)
                                                   .get(AuditJsonEnricher.NAME);
            assertEquals(2, auditNodes.size());
            assertEquals("documentModified", auditNodes.get(0).get("eventId").asText());
            assertEquals("documentCreated", auditNodes.get(1).get("eventId").asText());
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

}
