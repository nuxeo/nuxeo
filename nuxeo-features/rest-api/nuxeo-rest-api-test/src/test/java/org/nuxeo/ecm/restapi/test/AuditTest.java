/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.restapi.server.jaxrs.adapters.AuditAdapter;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @since 5.7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class, AuditFeature.class })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class AuditTest extends BaseTest {

    @Inject
    AuditLogger auditLogger;

    @Test
    public void shouldRetrieveAllLogEntries() throws Exception {
        DocumentModel doc = RestServerInit.getFile(1, session);

        ClientResponse response = getResponse(BaseTest.RequestType.GET, "id/"
                + doc.getId() + "/@" + AuditAdapter.NAME);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        List<JsonNode> nodes = getLogEntries(node);
        assertEquals(2, nodes.size());
        assertEquals("documentModified",
                nodes.get(0).get("eventId").getValueAsText());
        assertEquals("documentCreated",
                nodes.get(1).get("eventId").getValueAsText());
    }

    @Test
    public void shouldFilterLogEntriesOnEventId() throws Exception {
        DocumentModel doc = RestServerInit.getFile(1, session);

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("eventId", "documentModified");
        ClientResponse response = getResponse(BaseTest.RequestType.GET, "id/"
                + doc.getId() + "/@" + AuditAdapter.NAME, queryParams);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        List<JsonNode> nodes = getLogEntries(node);
        assertEquals(1, nodes.size());
        assertEquals("documentModified",
                nodes.get(0).get("eventId").getValueAsText());

        queryParams.putSingle("principalName", "bender");
        response = getResponse(BaseTest.RequestType.GET, "id/" + doc.getId()
                + "/@" + AuditAdapter.NAME, queryParams);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        nodes = getLogEntries(node);
        assertEquals(0, nodes.size());
    }

    @Test
    public void shouldFilterLogEntriesOnPrincipalName() throws Exception {
        DocumentModel doc = RestServerInit.getFile(1, session);

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("principalName", "Administrator");
        ClientResponse response = getResponse(BaseTest.RequestType.GET, "id/"
                + doc.getId() + "/@" + AuditAdapter.NAME, queryParams);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        List<JsonNode> nodes = getLogEntries(node);
        assertEquals(2, nodes.size());

        queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("principalName", "bender");
        response = getResponse(BaseTest.RequestType.GET, "id/" + doc.getId()
                + "/@" + AuditAdapter.NAME, queryParams);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        nodes = getLogEntries(node);
        assertEquals(0, nodes.size());
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
        ClientResponse response = getResponse(BaseTest.RequestType.GET, "id/"
                + doc.getId() + "/@" + AuditAdapter.NAME, queryParams);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        List<JsonNode> nodes = getLogEntries(node);
        assertEquals(3, nodes.size());

        queryParams = new MultivaluedMapImpl();
        queryParams.add("category", "Two");
        response = getResponse(BaseTest.RequestType.GET, "id/" + doc.getId()
                + "/@" + AuditAdapter.NAME, queryParams);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        nodes = getLogEntries(node);
        assertEquals(1, nodes.size());
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
        queryParams.add("startEventDate",
                ISODateTimeFormat.date().print(firstDate.minusDays(1)));
        ClientResponse response = getResponse(BaseTest.RequestType.GET, "id/"
                + doc.getId() + "/@" + AuditAdapter.NAME, queryParams);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        List<JsonNode> nodes = getLogEntries(node);
        assertEquals(3, nodes.size());

        queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("category", "One");
        queryParams.add("startEventDate",
                ISODateTimeFormat.date().print(firstDate.minusDays(1)));
        queryParams.add("endEventDate",
                ISODateTimeFormat.date().print(secondDate.minusDays(1)));
        response = getResponse(BaseTest.RequestType.GET, "id/" + doc.getId()
                + "/@" + AuditAdapter.NAME, queryParams);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        nodes = getLogEntries(node);
        assertEquals(2, nodes.size());

        queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("category", "One");
        queryParams.add("startEventDate",
                ISODateTimeFormat.date().print(firstDate.plusDays(1)));
        queryParams.add("endEventDate",
                ISODateTimeFormat.date().print(secondDate.plusDays(1)));
        response = getResponse(BaseTest.RequestType.GET, "id/" + doc.getId()
                + "/@" + AuditAdapter.NAME, queryParams);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        nodes = getLogEntries(node);
        assertEquals(1, nodes.size());
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
        ClientResponse response = getResponse(BaseTest.RequestType.GET, "id/"
                + doc.getId() + "/@" + AuditAdapter.NAME, queryParams);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        List<JsonNode> nodes = getLogEntries(node);
        assertEquals(3, nodes.size());

        queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("category", "One");
        queryParams.add("principalName", "leela");
        queryParams.add("eventId", "thirdEvent");
        response = getResponse(BaseTest.RequestType.GET, "id/" + doc.getId()
                + "/@" + AuditAdapter.NAME, queryParams);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        nodes = getLogEntries(node);
        assertEquals(1, nodes.size());

        queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("category", "One");
        queryParams.add("principalName", "leela");
        queryParams.add("eventId", "thirdEvent");
        queryParams.add("startEventDate",
                ISODateTimeFormat.date().print(firstDate.plusDays(1)));
        queryParams.add("endEventDate",
                ISODateTimeFormat.date().print(secondDate.minus(1)));
        response = getResponse(BaseTest.RequestType.GET, "id/" + doc.getId()
                + "/@" + AuditAdapter.NAME, queryParams);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        nodes = getLogEntries(node);
        assertEquals(0, nodes.size());
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
        ClientResponse response = getResponse(BaseTest.RequestType.GET, "id/"
                + doc.getId() + "/@" + AuditAdapter.NAME, queryParams);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        List<JsonNode> nodes = getLogEntries(node);
        assertEquals(6, nodes.size());

        queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("category", "One");
        queryParams.putSingle("currentPageIndex", "0");
        queryParams.putSingle("pageSize", "2");
        response = getResponse(BaseTest.RequestType.GET, "id/" + doc.getId()
                + "/@" + AuditAdapter.NAME, queryParams);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertTrue(node.get("isPaginable").getBooleanValue());
        assertEquals(0, node.get("currentPageIndex").getIntValue());
        assertEquals(2, node.get("pageSize").getIntValue());
        assertEquals(3, node.get("numberOfPages").getIntValue());
        nodes = getLogEntries(node);
        assertEquals(2, nodes.size());
        assertEquals("sixthEvent", nodes.get(0).get("eventId").getValueAsText());
        assertEquals("fifthEvent",
                nodes.get(1).get("eventId").getValueAsText());

        queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("category", "One");
        queryParams.putSingle("currentPageIndex", "1");
        queryParams.putSingle("pageSize", "3");
        response = getResponse(BaseTest.RequestType.GET, "id/" + doc.getId()
                + "/@" + AuditAdapter.NAME, queryParams);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertTrue(node.get("isPaginable").getBooleanValue());
        assertEquals(1, node.get("currentPageIndex").getIntValue());
        assertEquals(3, node.get("pageSize").getIntValue());
        assertEquals(2, node.get("numberOfPages").getIntValue());
        nodes = getLogEntries(node);
        assertEquals(3, nodes.size());
        assertEquals("thirdEvent",
                nodes.get(0).get("eventId").getValueAsText());
        assertEquals("secondEvent", nodes.get(1).get("eventId").getValueAsText());
        assertEquals("firstEvent", nodes.get(2).get("eventId").getValueAsText());

        queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("category", "One");
        queryParams.putSingle("currentPageIndex", "2");
        queryParams.putSingle("pageSize", "3");
        response = getResponse(BaseTest.RequestType.GET, "id/" + doc.getId()
                + "/@" + AuditAdapter.NAME, queryParams);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        nodes = getLogEntries(node);
        assertTrue(node.get("isPaginable").getBooleanValue());
        assertEquals(0, nodes.size());
    }

    @Override
    protected List<JsonNode> getLogEntries(JsonNode node) {
        assertEquals("logEntries", node.get("entity-type").getValueAsText());
        assertTrue(node.get("entries").isArray());
        List<JsonNode> result = new ArrayList<>();
        Iterator<JsonNode> elements = node.get("entries").getElements();
        while (elements.hasNext()) {
            result.add(elements.next());
        }
        return result;
    }

}
