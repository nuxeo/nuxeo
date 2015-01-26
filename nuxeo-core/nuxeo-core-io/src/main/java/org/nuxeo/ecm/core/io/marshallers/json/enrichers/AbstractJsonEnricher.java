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

package org.nuxeo.ecm.core.io.marshallers.json.enrichers;

import java.io.IOException;
import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;

/**
 * Base class to write {@link ExtensibleEntityJsonWriter}'s enricher.
 *
 * @param <EntityType> The Java type whose the generated JSON will be enriched.
 * @since 7.2
 */
public abstract class AbstractJsonEnricher<EntityType> extends AbstractJsonWriter<Enriched<EntityType>> {

    public static final String ENTITY_ENRICHER_NAME = "_EntityEnricherName";

    private final String name;

    public AbstractJsonEnricher(String name) {
        this.name = name;
    }

    @Override
    public final boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
        return name.equals(ctx.<String> getParameter(ENTITY_ENRICHER_NAME));
    }

    @Override
    public void write(Enriched<EntityType> enrichable, JsonGenerator jg) throws IOException {
        write(jg, enrichable.getEntity());
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
