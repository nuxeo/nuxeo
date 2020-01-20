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

import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.FETCH_PROPERTIES;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.MAX_DEPTH_PARAM;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.TRANSLATE_PROPERTIES;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import javax.inject.Inject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.DocumentPartImpl;
import org.nuxeo.ecm.core.api.model.impl.PropertyFactory;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.ListTypeImpl;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.SchemaImpl;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.directory.io.DirectoryEntryJsonWriter;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.query.core.BucketRange;
import org.nuxeo.ecm.platform.query.core.BucketRangeDate;
import org.nuxeo.elasticsearch.aggregate.SignificantTermAggregate;
import org.nuxeo.elasticsearch.aggregate.SingleBucketAggregate;
import org.nuxeo.elasticsearch.aggregate.SingleValueMetricAggregate;
import org.nuxeo.elasticsearch.aggregate.TermAggregate;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 8.4
 */
@SuppressWarnings("rawtypes")
@Setup(mode = SINGLETON, priority = REFERENCE)
public class AggregateJsonWriter extends ExtensibleEntityJsonWriter<Aggregate> {

    public static final String ENTITY_TYPE = "aggregate";

    public static final String FETCH_KEY = "key";

    private static final Log log = LogFactory.getLog(AggregateJsonWriter.class);

    /** Fake schema for system properties usable as a page provider aggregate */
    protected static final Schema SYSTEM_SCHEMA = new SchemaImpl("system", null);

    static {
        SYSTEM_SCHEMA.addField("ecm:mixinType", new ListTypeImpl("system", "", StringType.INSTANCE), null, 0, null);
        SYSTEM_SCHEMA.addField("ecm:tag", new ListTypeImpl("system", "", StringType.INSTANCE), null, 0, null);
        SYSTEM_SCHEMA.addField("ecm:primaryType", StringType.INSTANCE, null, 0, null);
        SYSTEM_SCHEMA.addField("ecm:currentLifeCycleState", StringType.INSTANCE, null, 0, null);
        SYSTEM_SCHEMA.addField("ecm:versionLabel", StringType.INSTANCE, null, 0, null);
        SYSTEM_SCHEMA.addField("ecm:isProxy", BooleanType.INSTANCE, null, 0, null);
        SYSTEM_SCHEMA.addField("ecm:isRecord", BooleanType.INSTANCE, null, 0, null);
        SYSTEM_SCHEMA.addField("ecm:retainUntil", DateType.INSTANCE, null, 0, null);
        SYSTEM_SCHEMA.addField("ecm:hasLegalHold", BooleanType.INSTANCE, null, 0, null);
        SYSTEM_SCHEMA.addField("ecm:isTrashed", BooleanType.INSTANCE, null, 0, null);
    }

    /**
     * @since 10.3
     */
    protected Field getSystemField(String name) {
        Field result = SYSTEM_SCHEMA.getField(name);
        if (result == null && name.startsWith("ecm:path@level")) {
            SYSTEM_SCHEMA.addField(name, StringType.INSTANCE, null, 0, null);
            return SYSTEM_SCHEMA.getField(name);
        }
        return result;
    }

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

    @SuppressWarnings("unchecked")
    @Override
    protected void writeEntityBody(Aggregate agg, JsonGenerator jg) throws IOException {
        boolean fetch = ctx.getFetched(ENTITY_TYPE).contains(FETCH_KEY);
        String fieldName = agg.getField();
        Field field;
        if (fieldName.startsWith("ecm:")) {
            field = getSystemField(fieldName);
            if (field == null) {
                log.warn(String.format("%s is not a valid field for aggregates", fieldName));
                return;
            }
        } else {
            field = schemaManager.getField(agg.getXPathField());
        }
        jg.writeObjectField("id", agg.getId());
        jg.writeObjectField("field", agg.getField());
        jg.writeObjectField("properties", agg.getProperties());
        jg.writeObjectField("ranges", agg.getRanges());
        jg.writeObjectField("selection", agg.getSelection());
        jg.writeObjectField("type", agg.getType());
        if (agg instanceof SingleValueMetricAggregate) {
            Double val = ((SingleValueMetricAggregate) agg).getValue();
            jg.writeObjectField("value", Double.isFinite(val) ? val : null);
        } else if (agg instanceof SingleBucketAggregate) {
            jg.writeObjectField("value", ((SingleBucketAggregate) agg).getDocCount());
        } else if (!fetch || !(agg instanceof TermAggregate || agg instanceof SignificantTermAggregate)) {
            jg.writeObjectField("buckets", agg.getBuckets());
            jg.writeObjectField("extendedBuckets", agg.getExtendedBuckets());
        } else {
            if (field != null) {
                try (Closeable resource = ctx.wrap()
                                             .with(FETCH_PROPERTIES + "." + DocumentModelJsonWriter.ENTITY_TYPE,
                                                     "properties")
                                             .with(FETCH_PROPERTIES + "." + DirectoryEntryJsonWriter.ENTITY_TYPE,
                                                     "parent")
                                             .with(TRANSLATE_PROPERTIES + "." + DirectoryEntryJsonWriter.ENTITY_TYPE,
                                                     "label")
                                             .with(MAX_DEPTH_PARAM, "max")
                                             .open()) {
                    // write buckets with privilege because we create a property to leverage marshallers
                    // copy the Framework#doPrivileged method to not change the IOException stack trace
                    try {
                        LoginContext loginContext = Framework.login();
                        try {
                            writeBuckets("buckets", agg.getBuckets(), field, jg);
                            writeBuckets("extendedBuckets", agg.getExtendedBuckets(), field, jg);
                        } finally {
                            if (loginContext != null) { // may be null in tests
                                loginContext.logout();
                            }
                        }
                    } catch (LoginException e) {
                        throw new NuxeoException(e);
                    }
                }
            } else {
                log.warn(String.format("Could not resolve field %s for aggregate %s", fieldName, agg.getId()));
                jg.writeObjectField("buckets", agg.getBuckets());
                jg.writeObjectField("extendedBuckets", agg.getExtendedBuckets());
            }
        }
    }

    protected void writeBuckets(String fieldName, List<Bucket> buckets, Field field, JsonGenerator jg)
            throws IOException, JsonGenerationException {
        // prepare document part in order to use property
        Schema schema = field.getDeclaringType().getSchema();
        DocumentPartImpl part = new DocumentPartImpl(schema);
        // write data
        jg.writeArrayFieldStart(fieldName);
        for (Bucket bucket : buckets) {
            jg.writeStartObject();

            jg.writeObjectField("key", bucket.getKey());

            Property prop = PropertyFactory.createProperty(part, field, Property.NONE);
            if (prop.isList()) {
                ListType t = (ListType) prop.getType();
                t.getField();
                prop = PropertyFactory.createProperty(part, t.getField(), Property.NONE);
            }
            log.debug(String.format("Writing %s for field %s resolved to %s", fieldName, field.getName().toString(),
                    prop.getName()));
            prop.setValue(bucket.getKey());

            writeEntityField("fetchedKey", prop, jg);
            jg.writeNumberField("docCount", bucket.getDocCount());
            jg.writeEndObject();

            if (bucket instanceof BucketRange) {
                BucketRange bucketRange = (BucketRange) bucket;
                jg.writeNumberField("from", bucketRange.getFrom());
                jg.writeNumberField("to", bucketRange.getTo());
            }

            if (bucket instanceof BucketRangeDate) {
                BucketRangeDate bucketRange = (BucketRangeDate) bucket;
                jg.writeStringField("fromAsDate", DateParser.formatW3CDateTime(bucketRange.getFromAsDate().toDate()));
                jg.writeStringField("toAsDate", DateParser.formatW3CDateTime(bucketRange.getToAsDate().toDate()));
            }
        }
        jg.writeEndArray();
    }

}
