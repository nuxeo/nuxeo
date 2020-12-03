/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 *     Ronan DANIELLOU <rdaniellou@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json.document;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter.ENTITY_TYPE;
import static org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher.ENTITY_ENRICHER_NAME;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ArrayProperty;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.OutputStreamWithJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.Enriched;
import org.nuxeo.ecm.core.io.registry.MarshallingException;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.context.WrappedContext;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.SimpleType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolver;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Convert {@link Property} to Json.
 * <p>
 * Format is:
 *
 * <pre>
 * "stringPropertyValue"  <-- for string property, each property may be fetched if a resolver is associated with that property and if a parameter fetch.document=propXPath is present, in this case, an object will be marshalled as value
 * or
 * true|false  <- for boolean property
 * or
 * 123  <- for int property
 * ...
 * {  <- for complex property
 *   "subProperty": ...,
 *    ...
 * },
 * [ ... ] <- for list property
 * }
 * </pre>
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DocumentPropertyJsonWriter extends AbstractJsonWriter<Property> {

    /**
     * Whether we should omit to write phantom secured properties.
     *
     * @since 11.1
     */
    public static final String OMIT_PHANTOM_SECURED_PROPERTY = "omitPhantomSecuredProperty";

    private static final Log log = LogFactory.getLog(DocumentPropertyJsonWriter.class);

    @Override
    public void write(Property prop, JsonGenerator jg) throws IOException {
        writeProperty(jg, prop);
        jg.flush();
    }

    protected void writeProperty(JsonGenerator jg, Property prop) throws IOException {
        if (prop.isScalar()) {
            writeScalarProperty(jg, prop);
        } else if (prop.isList()) {
            writeListProperty(jg, prop);
        } else if (prop instanceof BlobProperty) { // a blob
            writeBlobProperty(jg, (BlobProperty) prop);
        } else if (prop.isComplex()) {
            writeComplexProperty(jg, prop);
        } else if (prop.isPhantom()) {
            jg.writeNull();
        }
    }

    protected void writeScalarProperty(JsonGenerator jg, Property prop) throws IOException {
        Type type = prop.getType();
        Object value = prop.getValue();
        if (!fetchProperty(jg, prop.getType().getObjectResolver(), value, prop.getXPath())) {
            writeScalarPropertyValue(jg, ((SimpleType) type).getPrimitiveType(), value);
        }
    }

    private void writeScalarPropertyValue(JsonGenerator jg, Type type, Object value) throws IOException {
        if (value == null) {
            jg.writeNull();
        } else if (type instanceof BooleanType) {
            jg.writeBoolean((Boolean) value);
        } else if (type instanceof LongType) {
            jg.writeNumber(((Number) value).longValue()); // value may be a DeltaLong
        } else if (type instanceof DoubleType) {
            jg.writeNumber(((Number) value).doubleValue());
        } else if (type instanceof IntegerType) {
            jg.writeNumber(((Number) value).intValue());
        } else if (type instanceof BinaryType) {
            jg.writeBinary((byte[]) value);
        } else {
            jg.writeString(type.encode(value));
        }
    }

    protected boolean fetchProperty(JsonGenerator jg, ObjectResolver resolver, Object value, String path)
            throws IOException {
        if (value == null) {
            return false;
        }
        boolean fetched = false;
        if (resolver != null) {
            String genericPropertyPath = path.replaceAll("/[0-9]*/", "/*/");
            Set<String> fetchElements = ctx.getFetched(ENTITY_TYPE);
            boolean fetch = false;
            for (String fetchElement : fetchElements) {
                if ("properties".equals(fetchElement) || path.startsWith(fetchElement)
                        || genericPropertyPath.startsWith(fetchElement)) {
                    fetch = true;
                    break;
                }
            }
            if (fetch) {
                // use the current doc's session as the resolver context to fetch properties
                DocumentModel doc = ctx.getParameter(ENTITY_TYPE);
                CoreSession context = doc == null ? null : doc.getCoreSession();
                Object object = resolver.fetch(value, context);
                if (object != null) {
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        writeEntity(object, baos);
                        jg.writeRawValue(baos.toString());
                        fetched = true;
                    } catch (MarshallingException e) {
                        log.error("Unable to marshall as json the entity referenced by the property " + path, e);
                    }
                }
            }
        }
        return fetched;
    }

    protected void writeListProperty(JsonGenerator jg, Property prop) throws IOException {
        jg.writeStartArray();
        if (prop instanceof ArrayProperty) {
            Object[] ar = (Object[]) prop.getValue();
            if (ar == null) {
                jg.writeEndArray();
                return;
            }
            Type itemType = ((ListType) prop.getType()).getFieldType();
            ObjectResolver resolver = itemType.getObjectResolver();
            String path = prop.getXPath();
            for (Object o : ar) {
                if (!fetchProperty(jg, resolver, o, path)) {
                    writeScalarPropertyValue(jg, ((SimpleType) itemType).getPrimitiveType(), o);
                }
            }
        } else {
            ListProperty listp = (ListProperty) prop;
            for (Property p : listp.getChildren()) {
                writeProperty(jg, p);
            }
        }
        jg.writeEndArray();
    }

    protected void writeComplexProperty(JsonGenerator jg, Property prop) throws IOException {
        jg.writeStartObject();
        for (Property p : prop.getChildren()) {
            if (!DocumentPropertyJsonWriter.skipProperty(ctx, p)) {
                jg.writeFieldName(p.getName());
                writeProperty(jg, p);
            }
        }
        jg.writeEndObject();
    }

    protected void writeBlobProperty(JsonGenerator jg, BlobProperty prop) throws IOException {
        Blob blob = (Blob) prop.getValue();
        if (blob == null) {
            jg.writeNull();
            return;
        }
        jg.writeStartObject();
        String v = blob.getFilename();
        if (v == null) {
            jg.writeNullField("name");
        } else {
            jg.writeStringField("name", v);
        }
        v = blob.getMimeType();
        if (v == null) {
            jg.writeNullField("mime-type");
        } else {
            jg.writeStringField("mime-type", v);
        }
        v = blob.getEncoding();
        if (v == null) {
            jg.writeNullField("encoding");
        } else {
            jg.writeStringField("encoding", v);
        }
        v = blob.getDigestAlgorithm();
        if (v == null) {
            jg.writeNullField("digestAlgorithm");
        } else {
            jg.writeStringField("digestAlgorithm", v);
        }
        v = blob.getDigest();
        if (v == null) {
            jg.writeNullField("digest");
        } else {
            jg.writeStringField("digest", v);
        }
        jg.writeStringField("length", Long.toString(blob.getLength()));

        String blobUrl = getBlobUrl(prop);
        jg.writeStringField("data", blobUrl);

        enrichBlobProperty(jg, prop);

        jg.writeEndObject();
    }

    /**
     * @since 10.3
     */
    private void enrichBlobProperty(JsonGenerator jg, BlobProperty property) throws IOException {
        Set<String> enrichers = ctx.getEnrichers("blob");
        if (!enrichers.isEmpty()) {
            WrappedContext wrappedCtx = ctx.wrap();
            OutputStreamWithJsonWriter out = new OutputStreamWithJsonWriter(jg);
            Enriched<BlobProperty> enriched = new Enriched<>(property);
            ParameterizedType genericType = TypeUtils.parameterize(Enriched.class, BlobProperty.class);
            for (String enricherName : enrichers) {
                try (Closeable ignored = wrappedCtx.with(ENTITY_ENRICHER_NAME, enricherName).open()) {
                    Collection<Writer<Enriched>> writers = registry.getAllWriters(ctx, Enriched.class, genericType,
                            APPLICATION_JSON_TYPE);
                    for (Writer<Enriched> writer : writers) {
                        writer.write(enriched, Enriched.class, genericType, APPLICATION_JSON_TYPE, out);
                    }
                }
            }
        }
    }

    /**
     * Gets the full URL of where a blob can be downloaded.
     *
     * @since 7.2
     */
    private String getBlobUrl(Property prop) {
        DocumentModel doc = ctx.getParameter(ENTITY_TYPE);
        if (doc == null) {
            return "";
        }
        DownloadService downloadService = Framework.getService(DownloadService.class);

        String xpath = prop.getXPath();
        // if no prefix, use schema name as prefix:
        if (!xpath.contains(":")) {
            xpath = prop.getSchema().getName() + ":" + xpath;
        }

        Blob blob = (Blob) prop.getValue();
        return downloadService.getFullDownloadUrl(doc, xpath, blob, ctx.getBaseUrl());
    }

    protected static boolean skipProperty(RenderingContext ctx, Property property) {
        return ctx.getBooleanParameter(OMIT_PHANTOM_SECURED_PROPERTY) && property.isSecured() && property.isPhantom();
    }

}
