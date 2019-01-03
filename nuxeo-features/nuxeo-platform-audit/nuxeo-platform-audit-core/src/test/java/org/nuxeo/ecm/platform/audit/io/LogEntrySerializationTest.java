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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features(AuditFeature.class)
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

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        Date date1 = Date.from(
                LocalDate.parse("21-12-2012", formatter).atStartOfDay(ZoneId.systemDefault()).toInstant());
        extendedInfo.put(DATE_INFO, new ExtendedInfoImpl.DateInfo(date1));

        Date date2 = Date.from(Instant.parse("2012-01-01T00:00:00.000Z"));
        extendedInfo.put(DATE_INFO + "2", new ExtendedInfoImpl.DateInfo(date2));

        StringBlob stringBlob = new StringBlob("I'm a blob!");
        extendedInfo.put(BLOB_INFO, new ExtendedInfoImpl.BlobInfo(stringBlob));

        byte[] bytes = new byte[256];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }
        Blob blob = Blobs.createBlob(bytes);
        extendedInfo.put(BLOB_INFO + "2", new ExtendedInfoImpl.BlobInfo((Serializable) blob));

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
        assertEquals(date1.toInstant().toEpochMilli(),
                Instant.parse(jsonExtendedInfo.get(DATE_INFO).asText()).toEpochMilli());
        assertEquals(date2.toInstant().toEpochMilli(),
                Instant.parse(jsonExtendedInfo.get(DATE_INFO + "2").asText()).toEpochMilli());

        Serializable obj = SerializationUtils.deserialize(
                Base64.decodeBase64(jsonExtendedInfo.get(BLOB_INFO).binaryValue()));
        assertEquals("I'm a blob!", ((StringBlob) obj).getString());

        Serializable obj2 = SerializationUtils.deserialize(
                Base64.decodeBase64(jsonExtendedInfo.get(BLOB_INFO + "2").binaryValue()));
        assertArrayEquals(bytes, ((Blob) obj2).getByteArray());

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
                Base64.encodeBase64(SerializationUtils.serialize(new StringBlob("I'm a blob!"))));

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
        assertEquals(1325376000000L,
                ((ExtendedInfoImpl.DateInfo) infos.get(DATE_INFO)).getDateValue().toInstant().toEpochMilli());

        assertTrue(infos.get(BLOB_INFO) instanceof ExtendedInfoImpl.BlobInfo);
        assertEquals("I'm a blob!", ((StringBlob) infos.get(BLOB_INFO).getSerializableValue()).getString());
    }
}
