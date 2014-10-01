package org.nuxeo.elasticsearch.io;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;

public class AuditEntryJSONReader {
   
    public static LogEntry read(String content) throws Exception {

        JsonFactory factory = new JsonFactory();
            
        factory.setCodec(new ObjectMapper());
        JsonParser jp = factory.createJsonParser(content);
        
        JsonToken tok = jp.nextToken();

        // skip {
        if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
            tok = jp.nextToken();
        }

        LogEntryImpl entry = new LogEntryImpl();        
        
        while (tok != null && tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            jp.nextToken();
            if ("category".equals(key)) {
                entry.setCategory(jp.getText());
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
                entry.setExtendedInfos(readExtendedInfo(entry, jp));
            };
            tok = jp.nextToken();
        }        
        return entry;
    }    

    public static Map<String, ExtendedInfo> readExtendedInfo(LogEntryImpl entry, JsonParser jp) throws Exception {
        JsonToken tok = jp.nextToken();
        
        // skip {
        if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
            tok = jp.nextToken();
        }
        
        Map<String, ExtendedInfo> info = new HashMap<String, ExtendedInfo>();
        
        while (tok != null && tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            jp.nextToken();
            info.put(key,  ExtendedInfoImpl.createExtendedInfo((Serializable)jp.getText()));
            
            tok = jp.nextToken();
        }
        return info;        
    }

}
