/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json.document;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.WILDCARD_VALUE;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Set;

import javax.inject.Inject;

import org.codehaus.jackson.JsonGenerator;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.OutputStreamWithJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.MaxDepthReachedException;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.runtime.api.Framework;

import com.thoughtworks.xstream.io.json.JsonWriter;

/**
 * Convert {@link DocumentModel} to Json.
 * <p>
 * This marshaller is enrichable: register class implementing {@link AbstractJsonEnricher} and managing
 * {@link DocumentModel}.
 * </p>
 * <p>
 * This marshaller is also extensible: extend it and simply override
 * {@link ExtensibleEntityJsonWriter#extend(DocumentModel, JsonWriter)}.
 * </p>
 * <p>
 * Format is:
 *
 * <pre>
 * {
 *   "entity-type":"document",
 *   "repository": "REPOSITORY_NAME",
 *   "uid": "DOCUMENT_UID",
 *   "path": "DOCUMENT_PATH",
 *   "type": "DOCUMENT_TYPE",
 *   "state": "DOCUMENT_STATE",
 *   "parentRef": "PARENT_DOCUMENT_UID",
 *   "isCheckedOut": true|false,
 *   "changeToken": null|"CHANGE_TOKEN",
 *   "isCheckedOut": true|false,
 *   "title": "DOCUMENT_TITLE",
 *   "lastModified": "DATE_UPDATE",  <-- if dublincore is present and if dc:modified is not null
 *   "versionLabel": "DOCUMENT_VERSION",  <-- only activated with parameter fetch.document=versionLabel or system property nuxeo.document.json.fetch.heavy=true
 *   "lockOwner": "LOCK_OWNER",  <-- only activated if locked and with parameter fetch.document=lock or system property nuxeo.document.json.fetch.heavy=true
 *   "lockCreated": "LOCK_DATE",  <-- only activated if locked and with parameter fetch.document=lock or system property nuxeo.document.json.fetch.heavy=true
 *   "properties": {   <-- only present with parameter properties=schema1,schema2,... see {@link DocumentPropertyJsonWriter} for format
 *     "schemaPrefix:stringProperty": "stringPropertyValue",  <-- each property may be fetched if a resolver is associated with that property and if a parameter fetch.document=propXPath is present, in this case, an object will be marshalled as value
 *     "schemaPrefix:booleanProperty": true|false,
 *     "schemaPrefix:integerProperty": 123,
 *     ...
 *     "schemaPrefix:complexProperty": {
 *        "subProperty": ...,
 *        ...
 *     },
 *     "schemaPrefix:listProperty": [
 *        ...
 *     ]
 *   }
 *             <-- contextParameters if there are enrichers activated
 *             <-- additional property provided by extend() method
 * }
 * </pre>
 *
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DocumentModelJsonWriter extends ExtensibleEntityJsonWriter<DocumentModel> {

    public static final String ENTITY_TYPE = "document";

    public static final String DOCUMENT_JSON_FETCH_HEAVY_KEY = "nuxeo.document.json.fetch.heavy";

    private static Boolean FETCH_HEAVY_VALUES = null;

    private static boolean fetchHeavy() {
        if (FETCH_HEAVY_VALUES == null) {
            try {
                FETCH_HEAVY_VALUES = Framework.isBooleanPropertyTrue("nuxeo.document.json.fetch.heavy");
            } catch (Exception e) {
                FETCH_HEAVY_VALUES = false;
            }
        }
        return FETCH_HEAVY_VALUES;
    }

    private boolean mustFetch(String name) {
        return ctx.getFetched(ENTITY_TYPE).contains(name) || fetchHeavy();
    }

    @Inject
    private SchemaManager schemaManager;

    public DocumentModelJsonWriter() {
        super(ENTITY_TYPE, DocumentModel.class);
    }

    @Override
    protected void writeEntityBody(DocumentModel doc, JsonGenerator jg) throws IOException {
        jg.writeStringField("repository", doc.getRepositoryName());
        jg.writeStringField("uid", doc.getId());
        jg.writeStringField("path", doc.getPathAsString());
        jg.writeStringField("type", doc.getType());
        jg.writeStringField("state", doc.getRef() != null ? doc.getCurrentLifeCycleState() : null);
        jg.writeStringField("parentRef", doc.getParentRef() != null ? doc.getParentRef().toString() : null);
        jg.writeBooleanField("isCheckedOut", doc.isCheckedOut());
        jg.writeStringField("changeToken", doc.getChangeToken());
        jg.writeStringField("title", doc.getTitle());
        if (mustFetch("versionLabel")) {
            String versionLabel = doc.getVersionLabel();
            jg.writeStringField("versionLabel", versionLabel != null ? versionLabel : "");
        }
        if (mustFetch("lock")) {
            Lock lock = doc.getLockInfo();
            if (lock != null) {
                jg.writeStringField("lockOwner", lock.getOwner());
                jg.writeStringField("lockCreated", ISODateTimeFormat.dateTime().print(new DateTime(lock.getCreated())));
            }
        }
        if (doc.hasSchema("dublincore")) {
            Calendar cal = (Calendar) doc.getPropertyValue("dc:modified");
            if (cal != null) {
                jg.writeStringField("lastModified", DateParser.formatW3CDateTime(cal.getTime()));
            }
        }

        try (Closeable resource = ctx.wrap().controlDepth().open()) {
            Set<String> schemas = ctx.getProperties();
            if (schemas.size() > 0) {
                jg.writeObjectFieldStart("properties");
                if (schemas.contains(WILDCARD_VALUE)) {
                    // full document
                    for (String schema : doc.getSchemas()) {
                        writeSchemaProperties(jg, doc, schema);
                    }
                } else {
                    for (String schema : schemas) {
                        if (doc.hasSchema(schema)) {
                            writeSchemaProperties(jg, doc, schema);
                        }
                    }
                }
                jg.writeEndObject();
            }
        } catch (MaxDepthReachedException e) {
            // do not load properties
        }

        jg.writeArrayFieldStart("facets");
        for (String facet : doc.getFacets()) {
            jg.writeString(facet);
        }
        jg.writeEndArray();
    }

    private void writeSchemaProperties(JsonGenerator jg, DocumentModel doc, String schemaName) throws IOException {
        Writer<Property> propertyWriter = registry.getWriter(ctx, Property.class, APPLICATION_JSON_TYPE);
        // provides the current document to the property marshaller
        try (Closeable resource = ctx.wrap().with(ENTITY_TYPE, doc).open()) {
            Schema schema = schemaManager.getSchema(schemaName);
            String prefix = schema.getNamespace().prefix;
            if (prefix == null || prefix.length() == 0) {
                prefix = schemaName;
            }
            prefix = prefix + ":";
            for (Field field : schema.getFields()) {
                String prefixedName = prefix + field.getName().getLocalName();
                jg.writeFieldName(prefixedName);
                Property property = doc.getProperty(prefixedName);
                OutputStream out = new OutputStreamWithJsonWriter(jg);
                propertyWriter.write(property, Property.class, Property.class, APPLICATION_JSON_TYPE, out);
            }
        }
    }

}