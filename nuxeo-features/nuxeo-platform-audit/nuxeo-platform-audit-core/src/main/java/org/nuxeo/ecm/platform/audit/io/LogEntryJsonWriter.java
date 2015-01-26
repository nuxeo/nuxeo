/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.platform.audit.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.audit.api.LogEntry;

import com.thoughtworks.xstream.io.json.JsonWriter;

/**
 * Convert {@link LogEntry} to Json.
 * <p>
 * This marshaller is enrichable: register class implementing {@link AbstractJsonEnricher} and managing {@link LogEntry}
 * .
 * </p>
 * <p>
 * This marshaller is also extensible: extend it and simply override
 * {@link ExtensibleEntityJsonWriter#extend(LogEntry, JsonWriter)}.
 * </p>
 * <p>
 * Format is:
 *
 * <pre>
 * {@code
 * {
 *   "entity-type":"logEntry",
 *   "category": "LOG_ENTRY_CATEGORY",
 *   "principalName": "LOG_ENTRY_PRINCIPAL",
 *   "comment": "LOG_ENTRY_COMMENT",
 *   "docLifeCycle": "DOC_LIFECYCLE",
 *   "docPath": "DOC_PATH",
 *   "docType": "DOC_TYPE",
 *   "docUUID": "DOC_UUID",
 *   "eventId": "EVENT_ID",
 *   "repositoryId": "REPO_ID",
 *   "eventDate": "LOG_EVENT_DATE",
 *   "logDate": "LOG_DATE"
 *             <-- contextParameters if there are enrichers activated
 *             <-- additional property provided by extend() method
 * }
 * </pre>
 *
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class LogEntryJsonWriter extends ExtensibleEntityJsonWriter<LogEntry> {

    public static final String ENTITY_TYPE = "logEntry";

    public LogEntryJsonWriter() {
        super(ENTITY_TYPE, LogEntry.class);
    }

    @Override
    protected void writeEntityBody(LogEntry logEntry, JsonGenerator jg) throws IOException {
        jg.writeNumberField("id", logEntry.getId());
        jg.writeStringField("category", logEntry.getCategory());
        jg.writeStringField("principalName", logEntry.getPrincipalName());
        jg.writeStringField("comment", logEntry.getComment());
        jg.writeStringField("docLifeCycle", logEntry.getDocLifeCycle());
        jg.writeStringField("docPath", logEntry.getDocPath());
        jg.writeStringField("docType", logEntry.getDocType());
        jg.writeStringField("docUUID", logEntry.getDocUUID());
        jg.writeStringField("eventId", logEntry.getEventId());
        jg.writeStringField("repositoryId", logEntry.getRepositoryId());
        DateTimeFormatter dateTime = ISODateTimeFormat.dateTime();
        jg.writeStringField("eventDate", dateTime.print(new DateTime(logEntry.getEventDate())));
        jg.writeStringField("logDate", dateTime.print(new DateTime(logEntry.getLogDate())));
    }

}
