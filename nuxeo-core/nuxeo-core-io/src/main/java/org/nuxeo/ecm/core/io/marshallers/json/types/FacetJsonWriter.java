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

package org.nuxeo.ecm.core.io.marshallers.json.types;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.io.OutputStream;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.OutputStreamWithJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Schema;

import com.thoughtworks.xstream.io.json.JsonWriter;

/**
 * Convert {@link CompositeType} to Json.
 * <p>
 * This marshaller is enrichable: register class implementing {@link AbstractJsonEnricher} and managing
 * {@link CompositeType}.
 * </p>
 * <p>
 * This marshaller is also extensible: extend it and simply override
 * {@link ExtensibleEntityJsonWriter#extend(CompositeType, JsonWriter)}.
 * </p>
 * <p>
 * Format is:
 *
 * <pre>
 * {@code
 * {
 *   "entity-type":"facet",
 *   "name": "FACET_NAME",
 *   "schemas": [ { see {@link SchemaJsonWriter} for format }, { ... }, ... ],
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
public class FacetJsonWriter extends ExtensibleEntityJsonWriter<CompositeType> {

    public static final String ENTITY_TYPE = "facet";

    public FacetJsonWriter() {
        super(ENTITY_TYPE, CompositeType.class);
    }

    @Override
    protected void writeEntityBody(CompositeType facet, JsonGenerator jg) throws IOException {
        jg.writeStringField("name", facet.getName());
        if (facet.getSchemaNames() != null && facet.getSchemaNames().length > 0) {
            jg.writeArrayFieldStart("schemas");
            Writer<Schema> schemaWriter = registry.getWriter(ctx, Schema.class, APPLICATION_JSON_TYPE);
            for (Schema schema : facet.getSchemas()) {
                OutputStream out = new OutputStreamWithJsonWriter(jg);
                schemaWriter.write(schema, Schema.class, Schema.class, APPLICATION_JSON_TYPE, out);
            }
            jg.writeEndArray();
        }
    }

}
