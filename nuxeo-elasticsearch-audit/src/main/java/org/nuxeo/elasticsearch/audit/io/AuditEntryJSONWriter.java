package org.nuxeo.elasticsearch.audit.io;

import java.util.Map;

import org.codehaus.jackson.JsonGenerator;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;

public class AuditEntryJSONWriter {

    public static void asJSON(JsonGenerator jg,LogEntry logEntry) throws Exception  {
        jg.writeStartObject();
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
        
        
        Map<String, ExtendedInfo> extended = logEntry.getExtendedInfos();
        jg.writeObjectFieldStart("extended");
        for (String key : extended.keySet()) {
            jg.writeStringField(key, extended.get(key).getSerializableValue().toString());            
        }
        jg.writeEndObject();
        
        jg.writeEndObject();
        jg.flush();
    }
    
    
}
