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

package org.nuxeo.ecm.core.io.marshallers.json;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher.ENTITY_ENRICHER_NAME;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.ENTITY_FIELD_NAME;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.Enriched;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.MaxDepthReachedException;
import org.nuxeo.ecm.core.io.registry.context.WrappedContext;

/**
 * Base class to write Nuxeo Json entity. This class write the json object, the json "entity-type" property and enable
 * all activated enrichers registered in the {@link MarshallerRegistry} and compatible with the marshalled Java type.
 *
 * @param <EntityType> The managed Java type.
 * @since 7.2
 */
public abstract class ExtensibleEntityJsonWriter<EntityType> extends AbstractJsonWriter<EntityType> {

    /**
     * The "entity-type" Json property value.
     */
    private final String entityType;

    /**
     * The {@link Enriched} generic type parametrized with the given EntityType.
     */
    private final Type genericType;

    /**
     * @param entityType The "entity-type" Json property value.
     * @param entityClass The entity type.
     */
    public ExtensibleEntityJsonWriter(String entityType, Class<EntityType> entityClass) {
        super();
        this.entityType = entityType;
        genericType = TypeUtils.parameterize(Enriched.class, entityClass);
    }

    @Override
    public void write(EntityType entity, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeStringField(ENTITY_FIELD_NAME, entityType);
        writeEntityBody(entity, jg);
        try {
            WrappedContext wrappedCtx = ctx.wrap().controlDepth();
            Set<String> enrichers = ctx.getEnrichers(entityType);
            if (enrichers.size() > 0) {
                boolean hasEnrichers = false;
                Enriched<EntityType> enriched = null;
                for (String enricherName : enrichers) {
                    try (Closeable resource = wrappedCtx.with(ENTITY_ENRICHER_NAME, enricherName).open()) {
                        @SuppressWarnings("rawtypes")
                        Collection<Writer<Enriched>> writers = registry.getAllWriters(ctx, Enriched.class,
                                this.genericType, APPLICATION_JSON_TYPE);
                        for (@SuppressWarnings("rawtypes")
                        Writer<Enriched> writer : writers) {
                            if (!hasEnrichers) {
                                hasEnrichers = true;
                                jg.writeObjectFieldStart("contextParameters");
                                enriched = new Enriched<EntityType>(entity);
                            }
                            OutputStreamWithJsonWriter out = new OutputStreamWithJsonWriter(jg);
                            writer.write(enriched, Enriched.class, this.genericType, APPLICATION_JSON_TYPE, out);
                        }
                    }
                }
                if (hasEnrichers) {
                    jg.writeEndObject();
                }
            }
        } catch (MaxDepthReachedException e) {
            // do nothing, do not call enrichers
        }
        extend(entity, jg);
        jg.writeEndObject();
    }

    /**
     * Implement this method to write the entity body.
     *
     * @param entity The Java entity.
     * @param jg A {@link JsonGenerator} ready to write your entity as Json.
     * @since 7.2
     */
    protected abstract void writeEntityBody(EntityType entity, JsonGenerator jg) throws IOException;

    /**
     * Override this method to add additional property in the entity. This method is useful to override a marshaller
     * implementing this class.
     *
     * @param entity The Java entity.
     * @param jg A {@link JsonGenerator} ready to write your entity as Json.
     * @since 7.2
     */
    protected void extend(EntityType entity, JsonGenerator jg) throws IOException {
    }

}
