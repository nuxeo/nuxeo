/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;
import org.nuxeo.ecm.automation.client.model.RecordSet;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Manage JSON Decoding of RecordSet object returned by QueryAndFetch
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.7
 */
public class RecordSetMarshaller implements JsonMarshaller<RecordSet> {

    @Override
    public String getType() {
        return "recordSet";
    }

    @Override
    public Class<RecordSet> getJavaType() {
        return RecordSet.class;
    }

    @Override
    public RecordSet read(JsonParser jp) throws IOException {
        jp.nextToken();
        String key = jp.getCurrentName();
        if ("isPaginable".equals(key)) {
            jp.nextToken();
            boolean isPaginable = jp.getBooleanValue();
            if (isPaginable) {
                jp.nextToken();
                return readPaginableRecordSet(jp);
            }
        }
        return readRecord(jp);
    }

    protected RecordSet readPaginableRecordSet(JsonParser jp) throws IOException {
        RecordSet record = new RecordSet();
        JsonToken tok = jp.getCurrentToken();
        while (tok != null && tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            jp.nextToken();
            if ("pageSize".equals(key)) {
                record.setPageSize(jp.getIntValue());
            } else if ("numberOfPages".equals(key)) {
                record.setNumberOfPages(jp.getIntValue());
            } else if ("currentPageIndex".equals(key)) {
                record.setCurrentPageIndex(jp.getIntValue());
            } else if ("entries".equals(key)) {
                readRecordEntries(jp, record);
            }
            tok = jp.nextToken();
        }
        if (tok == null) {
            throw new IllegalArgumentException("Unexpected end of stream.");
        }
        return record;
    }

    protected RecordSet readRecord(JsonParser jp) throws IOException {
        RecordSet record = new RecordSet();
        JsonToken tok = jp.nextToken();
        while (tok != JsonToken.END_ARRAY) {
            String key = jp.getCurrentName();
            if ("entries".equals(key)) {
                readRecordEntries(jp, record);
                return record;
            }
            tok = jp.nextToken();
        }
        return record;
    }

    protected void readRecordEntries(JsonParser jp, RecordSet record) throws IOException {
        JsonToken tok = jp.nextToken();
        while (tok != JsonToken.END_ARRAY) {
            @SuppressWarnings("unchecked")
            Map<String, Serializable> entry = jp.readValueAs(Map.class);
            record.add(entry);
            tok = jp.nextToken();
        }
    }

    @Override
    public void write(JsonGenerator jg, Object value) throws IOException {
    }

}
