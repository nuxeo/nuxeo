/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.platform.audit.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @since 9.3
 */
public class LogEntrySerializationTest {

    protected static final String STRING_INFO = "stringInfo";

    protected static final String LONG_INFO = "longInfo";

    protected static final String DOUBLE_INFO = "doubleInfo";

    protected static final String BOOL_INFO = "boolInfo";

    protected static final String DATE_INFO = "dateInfo";

    protected static final String BLOB_INFO = "blobInfo";

    @Test
    public void testLogEntrySerialization() throws Exception {

        LogEntry entry = new LogEntryImpl();
        entry.setId(1L);

        Map<String, ExtendedInfo> extendedInfo = new HashMap<>();
        extendedInfo.put(STRING_INFO, new ExtendedInfoImpl.StringInfo("this is an info"));
        extendedInfo.put(LONG_INFO, new ExtendedInfoImpl.LongInfo(2L));
        extendedInfo.put(DOUBLE_INFO, new ExtendedInfoImpl.DoubleInfo(2.0));
        extendedInfo.put(BOOL_INFO, new ExtendedInfoImpl.BooleanInfo(true));

        String inputString = "21-12-2012";
        Date date = new SimpleDateFormat("dd-MM-yyyy").parse(inputString);
        extendedInfo.put(DATE_INFO, new ExtendedInfoImpl.DateInfo(date));

        StringBlob blob = new StringBlob("I'm a blob!");
        extendedInfo.put(BLOB_INFO, new ExtendedInfoImpl.BlobInfo(blob));

        entry.setExtendedInfos(extendedInfo);

        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(ExtendedInfo.class, new ExtendedInfoSerializer());
        mapper.registerModule(module);

        String json = mapper.writeValueAsString(entry);
        assertNotNull(json);

        JsonNode jsonLogEntry = mapper.readTree(json);
        assertEquals(1L, jsonLogEntry.get("id").asLong());

        JsonNode jsonExtendedInfo = jsonLogEntry.get("extended");

        assertEquals(2L, jsonExtendedInfo.get(LONG_INFO).asLong());
        assertEquals(2.0, jsonExtendedInfo.get(DOUBLE_INFO).asDouble(), 0);
        assertEquals("this is an info", jsonExtendedInfo.get(STRING_INFO).asText());
        assertEquals(true, jsonExtendedInfo.get(BOOL_INFO).asBoolean());
        assertEquals("2012-12-21T00:00:00Z", jsonExtendedInfo.get(DATE_INFO).asText());

        Serializable obj = ExtendedInfoDeserializer.deserializeFromByteArray(jsonExtendedInfo.get(BLOB_INFO).binaryValue());
        assertEquals("I'm a blob!", ((StringBlob) obj).getString());

    }

    @Test
    public void testLogEntryDeserialization() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(ExtendedInfo.class, new ExtendedInfoDeserializer());
        mapper.registerModule(module);

        ObjectNode logEntryJson = mapper.createObjectNode();

        logEntryJson.put("id", 1L);
        logEntryJson.with("extended").put(LONG_INFO, 2L);
        logEntryJson.with("extended").put(DOUBLE_INFO, 42.0);
        logEntryJson.with("extended").put(BOOL_INFO, false);
        logEntryJson.with("extended").put(STRING_INFO, "myInfo");
        logEntryJson.with("extended").put(DATE_INFO, "2012-01-01T00:00:00Z");
        logEntryJson.with("extended").put("blobInfo",
                ExtendedInfoSerializer.serializeToByteArray(new StringBlob("I'm a blob!")));

        LogEntryImpl logEntry = mapper.convertValue(logEntryJson, LogEntryImpl.class);

        Map<String, ExtendedInfo> infos = logEntry.getExtendedInfos();
        assertTrue(infos.get(LONG_INFO) instanceof ExtendedInfoImpl.LongInfo);
        assertEquals(2L, infos.get(LONG_INFO).getSerializableValue());

        assertTrue(infos.get(DOUBLE_INFO) instanceof ExtendedInfoImpl.DoubleInfo);
        assertEquals(42.0, infos.get(DOUBLE_INFO).getSerializableValue());

        assertTrue(infos.get(BOOL_INFO) instanceof ExtendedInfoImpl.BooleanInfo);
        assertEquals(false, infos.get(BOOL_INFO).getSerializableValue());

        assertTrue(infos.get(STRING_INFO) instanceof ExtendedInfoImpl.StringInfo);
        assertEquals("myInfo", infos.get(STRING_INFO).getSerializableValue());

        assertTrue(infos.get(DATE_INFO) instanceof ExtendedInfoImpl.DateInfo);

        assertTrue(infos.get(BLOB_INFO) instanceof ExtendedInfoImpl.BlobInfo);
        assertEquals("I'm a blob!", ((StringBlob) infos.get(BLOB_INFO).getSerializableValue()).getString());
    }
}
