package org.nuxeo.elasticsearch.audit.io;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.module.SimpleModule;
import org.elasticsearch.common.jackson.core.JsonProcessingException;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.core.api.impl.blob.AbstractBlob;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;

public class AuditEntryJSONWriter {

    protected static final Log log = LogFactory.getLog(AuditEntryJSONWriter.class);

    public static void asJSON(JsonGenerator jg, LogEntry logEntry) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("esAuditJson", org.codehaus.jackson.Version.unknownVersion());
        module.addSerializer(Map.class, new MapEntrySerializer());
        module.addSerializer(AbstractBlob.class, new BinaryBlobEntrySerializer());
        objectMapper.registerModule(module);
        jg.setCodec(objectMapper);

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
        jg.writeStringField("eventDate", ISODateTimeFormat.dateTime().print(new DateTime(logEntry.getEventDate())));
        jg.writeNumberField("id", logEntry.getId());
        jg.writeStringField("logDate", ISODateTimeFormat.dateTime().print(new DateTime(logEntry.getLogDate())));
        Map<String, ExtendedInfo> extended = logEntry.getExtendedInfos();
        jg.writeObjectFieldStart("extended");
        for (String key : extended.keySet()) {
            ExtendedInfo ei = extended.get(key);
            if (ei != null && ei.getSerializableValue() != null) {
                Serializable value = ei.getSerializableValue();
                if (value instanceof String) {
                    String strValue = ((String) value).trim();
                    if (strValue.startsWith("{") && strValue.endsWith("}")) {
                        jg.writeFieldName(key);
                        jg.writeRawValue(strValue);
                    } else if (strValue.startsWith("[") && strValue.endsWith("]")) {
                        jg.writeFieldName(key);
                        jg.writeRawValue(strValue);
                    } else {
                        jg.writeStringField(key,strValue);
                    }
                } else {
                    try {
                        jg.writeObjectField(key, ei.getSerializableValue());
                    } catch (JsonMappingException e) {
                        log.error("No Serializer found.", e);
                    }
                }
            } else {
                jg.writeNullField(key);
            }
        }
        jg.writeEndObject();

        jg.writeEndObject();
        jg.flush();
    }

    protected static void writeField(JsonGenerator jg, String name, String value) throws IOException {
        if (value == null) {
            jg.writeNullField(name);
        } else {
            jg.writeStringField(name, value);
        }
    }

    static class MapEntrySerializer extends JsonSerializer<Map> {

        @Override
        public void serialize(Map map, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            jgen.writeStartObject();
            for (Object key : map.keySet()) {
                jgen.writeObjectField((String)key, map.get(key));
            }
            jgen.writeEndObject();
        }
    }

    static class BinaryBlobEntrySerializer extends JsonSerializer<AbstractBlob> {

        @Override
        public void serialize(AbstractBlob blob, JsonGenerator jgen, SerializerProvider provider)
                throws JsonGenerationException, IOException {
            // Do not serizalize
            jgen.writeNull();
        }
    }

}
