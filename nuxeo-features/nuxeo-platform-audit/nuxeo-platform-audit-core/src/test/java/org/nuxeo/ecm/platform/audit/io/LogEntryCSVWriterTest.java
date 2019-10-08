/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nour AL KOTOB
 */
package org.nuxeo.ecm.platform.audit.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.platform.audit.io.LogEntryJsonWriter.ENTITY_TYPE;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.io.marshallers.csv.AbstractCSVWriterTest;
import org.nuxeo.ecm.core.io.marshallers.csv.CSVAssert;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 11.1
 */
@Features(AuditFeature.class)
public class LogEntryCSVWriterTest extends AbstractCSVWriterTest.External<LogEntryCSVWriter, LogEntry> {

    @Inject
    protected PageProviderService pps;

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature txFeature;

    protected DocumentModel file;

    public LogEntryCSVWriterTest() {
        super(LogEntryCSVWriter.class, LogEntry.class);
    }

    @Test
    public void testDefaultProperties() throws IOException {
        // No fetch param in the context, retrieve all default properties
        RenderingContext renderingCtx = RenderingContext.CtxBuilder.get();
        for (LogEntry entry : getLogEntries()) {
            CSVAssert csv = csvAssert(entry, renderingCtx);
            csv.has("id");
            csv.has("category").isEquals("eventDocumentCategory");
            csv.has("principalName").isEquals("Administrator"); // NOSONAR
            csv.has("comment").isEquals("");
            csv.has("docLifeCycle").isEquals("project"); // NOSONAR
            csv.has("docPath").isEquals("/myFile"); // NOSONAR
            csv.has("docType").isEquals("File"); // NOSONAR
            csv.has("docUUID").isEquals(file.getId()); // NOSONAR
            csv.has("eventId").isEquals(DocumentEventTypes.DOCUMENT_CREATED);
            csv.has("repositoryId").isEquals("test");
            csv.has("eventDate");
            csv.has("logDate");
        }
    }

    @Test
    public void testCustomProperties() throws IOException {
        // Fetching only specific properties via context param
        RenderingContext renderingCtx = RenderingContext.CtxBuilder.fetch(ENTITY_TYPE, "principalName", "docLifeCycle",
                "docPath", "docType", "docUUID").get();
        for (LogEntry entry : getLogEntries()) {
            CSVAssert csv = csvAssert(entry, renderingCtx);
            csv.has("principalName").isEquals("Administrator");
            csv.has("docLifeCycle").isEquals("project");
            csv.has("docPath").isEquals("/myFile");
            csv.has("docType").isEquals("File");
            csv.has("docUUID").isEquals(file.getId());
            // Default properties that are not requested and shouldn't be there
            List<String> unwantedProps = Arrays.asList("category", "comment", "eventId", "eventDate", "logDate");
            for (String property : unwantedProps) {
                // expected to catch
                try {
                    csv.has(property);
                    fail();
                } catch (AssertionError e) {
                    assertEquals("no field " + property, e.getMessage());
                }
            }
        }
    }

    protected List<LogEntry> getLogEntries() {
        file = session.createDocumentModel("/", "myFile", "File");
        file = session.createDocument(file);
        // LogEntry are event driven. They are created asynchronously
        txFeature.nextTransaction();
        PageProvider<?> pp = pps.getPageProvider("DOCUMENT_HISTORY_PROVIDER", null, Long.valueOf(20), Long.valueOf(0),
                new HashMap<>(), file);
        @SuppressWarnings("unchecked")
        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();
        // if there is nothing to test, the test would always succeed.
        assertFalse(entries.isEmpty());
        return entries;
    }
}
