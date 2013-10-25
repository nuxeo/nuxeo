/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.jaxrs.io.audit;

import java.io.IOException;

import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonGenerator;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.automation.jaxrs.io.EntityWriter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.audit.api.LogEntry;

/**
 * @since 5.7.3 - LogEntry Writer for Audit.
 */
@Provider
@Produces({ "application/json+nxentity", "application/json" })
public class LogEntryWriter extends EntityWriter<LogEntry> {

    public static final String ENTITY_TYPE = "logEntry";

    @Override
    protected String getEntityType() {
        return ENTITY_TYPE;
    }

    @Override
    protected void writeEntityBody(JsonGenerator jg, LogEntry logEntry)
            throws IOException, ClientException {
        jg.writeStringField("entity-type", "logEntry");
        jg.writeStringField("category", logEntry.getCategory());
        jg.writeStringField("principalName", logEntry.getPrincipalName());
        jg.writeStringField("comment", logEntry.getComment());
        jg.writeStringField("docLifeCycle", logEntry.getDocLifeCycle());
        jg.writeStringField("docPath", logEntry.getDocPath());
        jg.writeStringField("docType", logEntry.getDocType());
        jg.writeStringField("docUUID", logEntry.getDocUUID());
        jg.writeStringField("eventId", logEntry.getEventId());
        jg.writeStringField("repositoryId", logEntry.getRepositoryId());
        jg.writeStringField(
                "eventDate",
                ISODateTimeFormat.dateTime().print(
                        new DateTime(logEntry.getEventDate())));
        jg.writeNumberField("id", logEntry.getId());
        jg.writeStringField(
                "logDate",
                ISODateTimeFormat.dateTime().print(
                        new DateTime(logEntry.getLogDate())));
    }

}
