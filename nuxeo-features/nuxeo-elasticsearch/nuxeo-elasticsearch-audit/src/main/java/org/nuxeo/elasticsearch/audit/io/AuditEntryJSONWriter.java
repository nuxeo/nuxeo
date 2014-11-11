package org.nuxeo.elasticsearch.audit.io;

import java.util.Map;

import org.codehaus.jackson.JsonGenerator;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;

public class AuditEntryJSONWriter {

    public static void asJSON(JsonGenerator jg, LogEntry logEntry)
            throws Exception {
        jg.writeStartObject();
        jg.writeStringField("entity-type", "logEntry");

        writeField(jg, "category", logEntry.getCategory());
        writeField(jg, "principalName", logEntry.getPrincipalName());
        writeField(jg, "comment", logEntry.getComment());
        writeField(jg, "docLifeCycle", logEntry.getDocLifeCycle());
        writeField(jg, "docPath", logEntry.getDocPath());
        writeField(jg, "docType", logEntry.getDocType());
        writeField(jg, "docUUID", logEntry.getDocUUID());
        writeField(jg, "eventId", logEntry.getEventId());
        writeField(jg, "repositoryId", logEntry.getRepositoryId());
        jg.writeStringField(
                "eventDate",
                ISODateTimeFormat.dateTime().print(
                        new DateTime(logEntry.getEventDate())));
        jg.writeNumberField("id", logEntry.getId());
        jg.writeStringField(
                "logDate",
                ISODateTimeFormat.dateTime().print(
                        new DateTime(logEntry.getLogDate())));
        Map<String, ExtendedInfo> extended = logEntry.getExtendedInfos();
        jg.writeObjectFieldStart("extended");
        for (String key : extended.keySet()) {
            ExtendedInfo ei = extended.get(key);
            if (ei != null && ei.getSerializableValue() != null) {
                jg.writeStringField(key, ei.getSerializableValue().toString());
            } else {
                jg.writeNullField(key);
            }
        }
        jg.writeEndObject();

        jg.writeEndObject();
        jg.flush();
    }

    protected static void writeField(JsonGenerator jg, String name, String value)
            throws Exception {
        if (value == null) {
            jg.writeNullField(name);
        } else {
            jg.writeStringField(name, value);
        }
    }

}
