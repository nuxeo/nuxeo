package org.nuxeo.elasticsearch.audit.io;

import java.io.IOException;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.module.SimpleModule;
import org.elasticsearch.common.jackson.core.JsonProcessingException;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.core.blob.binary.BinaryBlob;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;

public class AuditEntryJSONWriter {

    public static void asJSON(JsonGenerator jg, LogEntry logEntry) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("esAuditJson", org.codehaus.jackson.Version.unknownVersion());
        module.addSerializer(Map.class, new MapEntrySerializer());
        module.addSerializer(BinaryBlob.class, new BinaryBlobEntrySerializer());
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

                jg.writeObjectField(key, ei.getSerializableValue());
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

    static class BinaryBlobEntrySerializer extends JsonSerializer<BinaryBlob> {

        @Override
        public void serialize(BinaryBlob binaryBlob, JsonGenerator jgen, SerializerProvider provider) throws JsonGenerationException, IOException {
            // Not supported
            jgen.writeNull();
        }
    }

}
