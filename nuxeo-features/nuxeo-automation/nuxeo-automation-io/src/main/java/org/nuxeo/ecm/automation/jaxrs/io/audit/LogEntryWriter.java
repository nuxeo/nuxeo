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
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonGenerator;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.automation.jaxrs.io.EntityWriter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.webengine.jaxrs.coreiodelegate.JsonCoreIODelegate;

/**
 * @since 5.7.3 - LogEntry Writer for Audit.
 * @deprecated this marshaller was migrated to org.nuxeo.ecm.platform.audit.io.LogEntryJsonWriter. To use it in JAX-RS,
 *             register the {@link JsonCoreIODelegate} to forward the JAX-RS marshalling to nuxeo-core-io.
 */
@Deprecated
@Provider
@Produces({ "application/json+nxentity", "application/json" })
public class LogEntryWriter extends EntityWriter<LogEntry> {

    public static final String ENTITY_TYPE = "logEntry";

    @Override
    protected String getEntityType() {
        return ENTITY_TYPE;
    }

    @Override
    protected void writeEntityBody(JsonGenerator jg, LogEntry logEntry) throws IOException, ClientException {
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
        jg.writeStringField("eventDate", ISODateTimeFormat.dateTime().print(new DateTime(logEntry.getEventDate())));
        jg.writeNumberField("id", logEntry.getId());
        jg.writeStringField("logDate", ISODateTimeFormat.dateTime().print(new DateTime(logEntry.getLogDate())));
        writeExtendedInfos(jg, logEntry);
    }

    protected void writeExtendedInfos(JsonGenerator jg, LogEntry logEntry) throws IOException, ClientException {
        Map<String, ExtendedInfo> extended = logEntry.getExtendedInfos();
        jg.writeObjectFieldStart("extended");
        for (String key : extended.keySet()) {
            ExtendedInfo ei = extended.get(key);
            if (ei != null && ei.getSerializableValue() != null) {
                writeExtendedInfo(jg, key, ei.getSerializableValue());
            } else {
                jg.writeNullField(key);
            }
        }
        jg.writeEndObject();
    }

    protected void writeExtendedInfo(JsonGenerator jg, String key, Serializable value) throws IOException,
            ClientException {
        Class<?> clazz = value.getClass();
        if (Long.class.isAssignableFrom(clazz)) {
            jg.writeNumberField(key, (Long) value);
        } else if (Integer.class.isAssignableFrom(clazz)) {
            jg.writeNumberField(key, (Integer) value);
        } else if (Double.class.isAssignableFrom(clazz)) {
            jg.writeNumberField(key, (Double) value);
        } else if (Date.class.isAssignableFrom(clazz)) {
            jg.writeStringField(key, ISODateTimeFormat.dateTime().print(new DateTime(value)));
        } else if (String.class.isAssignableFrom(clazz)) {
            jg.writeStringField(key, (String) value);
        } else if (Boolean.class.isAssignableFrom(clazz)) {
            jg.writeBooleanField(key, (Boolean) value);
        } else {
            jg.writeStringField(key, value.toString());
        }
    }
}
