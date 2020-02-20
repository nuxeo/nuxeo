/*
 * (C) Copyright 2015-2020 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.io.marshallers.json.enrichers;

import java.io.IOException;
import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * Base class to write {@link ExtensibleEntityJsonWriter}'s enricher.
 *
 * @param <EntityType> The Java type whose the generated JSON will be enriched.
 * @since 7.2
 */
public abstract class AbstractJsonEnricher<EntityType> extends AbstractJsonWriter<Enriched<EntityType>> {

    private static final Logger log = LogManager.getLogger(AbstractJsonEnricher.class);

    public static final String ENTITY_ENRICHER_NAME = "_EntityEnricherName";

    private final String name;

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    public AbstractJsonEnricher(String name) {
        this.name = name;
    }

    @Override
    public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
        return name.equals(ctx.<String> getParameter(ENTITY_ENRICHER_NAME));
    }

    @Override
    public void write(Enriched<EntityType> enrichable, JsonGenerator jg) {
        try (TokenBuffer tb = new TokenBuffer(MAPPER, false)) {
            // Write to a temporary output in case of exception during write()
            tb.writeStartObject();
            write(tb, enrichable.getEntity());
            tb.flush();
            // Add the complete, well-formed content to the real output
            try (JsonParser parser = tb.asParser()) {
                parser.nextToken(); // ignoring START_OBJECT
                while (parser.nextToken() == JsonToken.FIELD_NAME) {
                    jg.copyCurrentStructure(parser);
                }
                if (parser.currentToken() != null) {
                    log.error("Enricher {} returned invalid output {}", name, tb.toString());
                }
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("interrupted", e); // NOSONAR
            } else {
                // TODO collect exception and return it to the caller
                log.info("Enricher {} failed", name, e);
            }
        }
    }

    /**
     * When implementing this method, the provided {@link JsonGenerator} expect you write a field name and a field value
     * (or many).
     *
     * @param jg The {@link JsonGenerator} to use.
     * @param enriched The enriched entity.
     * @since 7.2
     */
    public abstract void write(JsonGenerator jg, EntityType enriched) throws IOException;

}
