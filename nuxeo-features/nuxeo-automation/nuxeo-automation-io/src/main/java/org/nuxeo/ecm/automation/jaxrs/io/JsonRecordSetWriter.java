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

package org.nuxeo.ecm.automation.jaxrs.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.util.PaginableRecordSet;
import org.nuxeo.ecm.automation.core.util.RecordSet;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Manage JSON Marshalling for {@link RecordSet} objects
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.7
 */
@Provider
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON + "+nxentity" })
public class JsonRecordSetWriter implements MessageBodyWriter<RecordSet> {

    protected static Log log = LogFactory.getLog(JsonRecordSetWriter.class);

    @Context
    JsonFactory factory;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        boolean canUse = RecordSet.class.isAssignableFrom(type);
        return canUse;
    }

    @Override
    public long getSize(RecordSet t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1L;
    }

    @Override
    public void writeTo(RecordSet records, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream out) throws IOException,
            WebApplicationException {
        try {
            writeRecords(out, records);
        } catch (IOException e) {
            log.error("Failed to serialize recordset", e);
            throw new WebApplicationException(500);
        }

    }

    protected void writeRecords(OutputStream out, RecordSet records) throws IOException {

        try (JsonGenerator jg = factory.createGenerator(out, JsonEncoding.UTF8)) {

            jg.writeStartObject();
            jg.writeStringField("entity-type", "recordSet");

            if (records instanceof PaginableRecordSet) {
                PaginableRecordSet pRecord = (PaginableRecordSet) records;
                jg.writeBooleanField("isPaginable", true);
                jg.writeNumberField("resultsCount", pRecord.getResultsCount());
                jg.writeNumberField("pageSize", pRecord.getPageSize());
                jg.writeNumberField("maxPageSize", pRecord.getMaxPageSize());
                jg.writeNumberField("currentPageSize", pRecord.getCurrentPageSize());
                jg.writeNumberField("currentPageIndex", pRecord.getCurrentPageIndex());
                jg.writeNumberField("numberOfPages", pRecord.getNumberOfPages());
                jg.writeBooleanField("isPreviousPageAvailable", pRecord.isPreviousPageAvailable());
                jg.writeBooleanField("isNextPageAvailable", pRecord.isNextPageAvailable());
                jg.writeBooleanField("isLastPageAvailable", pRecord.isLastPageAvailable());
                jg.writeBooleanField("isSortable", pRecord.isSortable());
                jg.writeBooleanField("hasError", pRecord.hasError());
                jg.writeStringField("errorMessage", pRecord.getErrorMessage());
            }

            jg.writeArrayFieldStart("entries");
            for (Map<String, Serializable> entry : records) {
                jg.writeObject(entry);
            }

            if (records instanceof PaginableRecordSet) {
                PaginableRecordSet pRecord = (PaginableRecordSet) records;
                if (pRecord.hasAggregateSupport() && pRecord.getAggregates() != null
                        && !pRecord.getAggregates().isEmpty()) {
                    jg.writeObjectField("aggregations", pRecord.getAggregates());
                }
            }

            jg.writeEndArray();
            jg.writeEndObject();
        }
    }

}
