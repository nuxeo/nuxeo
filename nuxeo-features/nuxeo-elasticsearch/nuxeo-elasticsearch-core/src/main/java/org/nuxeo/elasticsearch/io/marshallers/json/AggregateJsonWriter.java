/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <grenard@nuxeo.com>
 */
package org.nuxeo.elasticsearch.io.marshallers.json;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.MarshallingException;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolver;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;

@Setup(mode = SINGLETON, priority = REFERENCE)
public class AggregateJsonWriter extends ExtensibleEntityJsonWriter<Aggregate> {

    private static final Log log = LogFactory.getLog(AggregateJsonWriter.class);

    public static final String ENTITY_TYPE = "aggregate";

    public static final String FETCH_KEY = "key";

    @Inject
    private SchemaManager schemaManager;

    public AggregateJsonWriter() {
        super(ENTITY_TYPE, Aggregate.class);
    }

    public AggregateJsonWriter(String entityType, Class<Aggregate> entityClass) {
        super(entityType, entityClass);
    }

    @Override
    public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
        return true;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected void writeEntityBody(Aggregate agg, JsonGenerator jg) throws IOException {

        Set<String> fetchElements = ctx.getFetched(ENTITY_TYPE);
        boolean fetch = false;
        for (String fetchElement : fetchElements) {
            if (FETCH_KEY.equals(fetchElement)) {
                fetch = true;
                break;
            }
        }

        jg.writeObjectField("id", agg.getId());
        jg.writeObjectField("field", agg.getField());
        jg.writeObjectField("properties", agg.getProperties());
        jg.writeObjectField("ranges", agg.getRanges());
        jg.writeObjectField("selection", agg.getSelection());
        jg.writeObjectField("type", agg.getType());
        jg.writeObjectField("extendedBuckets", agg.getExtendedBuckets());
        if (!fetch) {
            jg.writeObjectField("buckets", agg.getBuckets());
        } else {
            String fieldName = agg.getField();
            Field field = schemaManager.getField(fieldName);
            List<Bucket> buckets = agg.getBuckets();
            // XXX Why the object resolver is null for coverage, and other fields that have one in dublincore schema
            // definition.?
            ObjectResolver or = field.getType().getObjectResolver();
            jg.writeArrayFieldStart("buckets");
            for (Bucket bucket : buckets) {
                jg.writeStartObject();
                jg.writeFieldName("key");
                writeFetchProperty(jg, or, bucket.getKey(), agg.getId());
                jg.writeNumberField("docCount", bucket.getDocCount());
                jg.writeEndObject();
            }
            jg.writeEndArray();
        }
    }

    protected void writeFetchProperty(JsonGenerator jg, ObjectResolver resolver, Object value, String aggName)
            throws IOException {
        if (value == null) {
            return;
        }
        if (resolver != null) {
            Object object = resolver.fetch(value);
            if (object != null) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    writeEntity(object, baos);
                    jg.writeRawValue(baos.toString());
                } catch (MarshallingException e) {
                    log.error("Unable to marshall as json the entity referenced by the aggregation " + aggName, e);
                }
            }
        } else {
            jg.writeString((String) value);
        }
    }

}
