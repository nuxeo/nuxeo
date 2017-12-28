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

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.Reader;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Base class to convert json as {@link List}.
 * <p>
 * It follow the classic Nuxeo list format :
 *
 * <pre>
 * {
 *   "entity-type": "GIVEN_ENTITY_TYPE",
 *   "entries": [
 *     {...}, <-- A {@link Reader} must be able to manage this format.
 *     {...},
 *     ...
 *     {...}
 *   ]
 * }
 * </pre>
 *
 * </p>
 * <p>
 * This reader delegates the unmarshalling of entries to the {@link MarshallerRegistry}. A Json {@link Reader}
 * compatible with the required type and the json format must be registered.
 * </p>
 *
 * @param <EntityType> The type of the element of this list.
 * @since 7.2
 */
public abstract class DefaultListJsonReader<EntityType> extends EntityJsonReader<List<EntityType>> {

    /**
     * The Java type of the element of this list.
     */
    private final Class<EntityType> elClazz;

    /**
     * The generic type of the element of this list.
     */
    private final Type elGenericType;

    /**
     * Use this constructor if the element of the list are not based on Java generic type.
     *
     * @param entityType The list "entity-type".
     * @param elClazz The class of the element of the list.
     */
    public DefaultListJsonReader(String entityType, Class<EntityType> elClazz) {
        super(entityType);
        this.elClazz = elClazz;
        elGenericType = elClazz;
    }

    /**
     * Use this constructor if the element of the list are based on Java generic type.
     *
     * @param entityType The list "entity-type".
     * @param elClazz The class of the element of the list.
     * @param elGenericType The generic type of the list (you can use {@link TypeUtils#parameterize(Class, Type...) to
     *            generate it}
     */
    public DefaultListJsonReader(String entityType, Class<EntityType> elClazz, Type elGenericType) {
        super(entityType);
        this.elClazz = elClazz;
        this.elGenericType = elGenericType;
    }

    @Override
    protected List<EntityType> readEntity(JsonNode jn) throws IOException {
        Reader<EntityType> entryReader = registry.getReader(ctx, elClazz, elGenericType, APPLICATION_JSON_TYPE);
        List<EntityType> result = new ArrayList<EntityType>();
        JsonNode entriesNode = jn.get("entries");
        if (entriesNode != null && !entriesNode.isNull() && entriesNode.isArray()) {
            JsonNode entryNode = null;
            Iterator<JsonNode> it = entriesNode.elements();
            while (it.hasNext()) {
                entryNode = it.next();
                InputStreamWithJsonNode in = new InputStreamWithJsonNode(entryNode);
                EntityType doc = entryReader.read(elClazz, elClazz, APPLICATION_JSON_TYPE, in);
                result.add(doc);
            }
        }
        return result;
    }

}
