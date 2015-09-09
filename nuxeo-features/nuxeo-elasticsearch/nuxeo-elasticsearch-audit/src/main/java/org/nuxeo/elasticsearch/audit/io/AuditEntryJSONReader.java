package org.nuxeo.elasticsearch.audit.io;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.core.api.impl.blob.AbstractBlob;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.elasticsearch.audit.io.AuditEntryJSONWriter.BinaryBlobEntrySerializer;
import org.nuxeo.elasticsearch.audit.io.AuditEntryJSONWriter.MapEntrySerializer;

public class AuditEntryJSONReader {

    public static LogEntry read(String content) throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("esAuditJson", org.codehaus.jackson.Version.unknownVersion());
        module.addSerializer(Map.class, new MapEntrySerializer());
        module.addSerializer(AbstractBlob.class, new BinaryBlobEntrySerializer());
        objectMapper.registerModule(module);

        JsonFactory factory = new JsonFactory();
        factory.setCodec(objectMapper);
        JsonParser jp = factory.createJsonParser(content);

        JsonToken tok = jp.nextToken();

        // skip {
        if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
            tok = jp.nextToken();
        }

        LogEntryImpl entry = new LogEntryImpl();

        while (tok != null && tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            JsonToken token = jp.nextToken();
            if (token != JsonToken.VALUE_NULL) {
                if ("category".equals(key)) {
                    entry.setCategory(token == JsonToken.VALUE_NULL ? null : jp.getText());
                } else if ("principalName".equals(key)) {
                    entry.setPrincipalName(jp.getText());
                } else if ("comment".equals(key)) {
                    entry.setComment(jp.getText());
                } else if ("docLifeCycle".equals(key)) {
                    entry.setDocLifeCycle(jp.getText());
                } else if ("docPath".equals(key)) {
                    entry.setDocPath(jp.getText());
                } else if ("docType".equals(key)) {
                    entry.setDocType(jp.getText());
                } else if ("docUUID".equals(key)) {
                    entry.setDocUUID(jp.getText());
                } else if ("eventId".equals(key)) {
                    entry.setEventId(jp.getText());
                } else if ("repositoryId".equals(key)) {
                    entry.setRepositoryId(jp.getText());
                } else if ("id".equals(key)) {
                    entry.setId(jp.getLongValue());
                } else if ("eventDate".equals(key)) {
                    entry.setEventDate(ISODateTimeFormat.dateTime().parseDateTime(jp.getText()).toDate());
                } else if ("logDate".equals(key)) {
                    entry.setLogDate(ISODateTimeFormat.dateTime().parseDateTime(jp.getText()).toDate());
                } else if ("extended".equals(key)) {
                    entry.setExtendedInfos(readExtendedInfo(entry, jp, objectMapper));
                }
            }
            tok = jp.nextToken();
        }
        return entry;
    }

    public static Map<String, ExtendedInfo> readExtendedInfo(LogEntryImpl entry, JsonParser jp,
            ObjectMapper objectMapper) throws IOException {

        Map<String, ExtendedInfo> info = new HashMap<String, ExtendedInfo>();

        JsonNode node = jp.readValueAsTree();

        Iterator<String> fieldsIt = node.getFieldNames();

        while (fieldsIt.hasNext()) {
            String fieldName = fieldsIt.next();

            JsonNode field = node.get(fieldName);

            if (field.isObject()) {
                info.put(fieldName, ExtendedInfoImpl.createExtendedInfo(objectMapper.writeValueAsString(field)));
            } else if (field.isArray()) {
                info.put(fieldName, ExtendedInfoImpl.createExtendedInfo(objectMapper.writeValueAsString(field)));
            } else {
                if (field.isInt() || field.isLong()) {
                    info.put(fieldName, ExtendedInfoImpl.createExtendedInfo(field.getLongValue()));
                } else {
                    info.put(fieldName, ExtendedInfoImpl.createExtendedInfo(field.getTextValue()));
                }
            }
        }
        return info;
    }

}
