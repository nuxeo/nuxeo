/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.core.io.marshallers.json;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher.ENTITY_ENRICHER_NAME;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.ENTITY_FIELD_NAME;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.Enriched;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.MaxDepthReachedException;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.context.WrappedContext;

import com.fasterxml.jackson.core.JsonGenerator;

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
        List<Object> entityList = new ArrayList<>();
        entityList.add(this.entityType);
        ctx.addParameterListValues(RenderingContext.RESPONSE_HEADER_ENTITY_TYPE_KEY, entityList);
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
