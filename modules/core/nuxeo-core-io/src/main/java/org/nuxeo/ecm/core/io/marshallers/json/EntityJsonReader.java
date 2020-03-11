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

import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.ENTITY_FIELD_NAME;

import java.io.IOException;

import org.nuxeo.ecm.core.io.registry.MarshallingException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Base class to read Nuxeo entity Json and convert it in Objects. This class checks the json is an object, the json
 * property "entity-type" is present and as expected and delegate the body reading to an abstract method.
 *
 * @param <EntityType> The managed Java type.
 * @since 7.2
 */
public abstract class EntityJsonReader<EntityType> extends AbstractJsonReader<EntityType> {

    /**
     * The expected "entity-type" property in the json.
     */
    private final String entityType;

    /**
     * @param entityType The expected "entity-type" property in the json.
     */
    public EntityJsonReader(String entityType) {
        super();
        this.entityType = entityType;
    }

    @Override
    public final EntityType read(JsonNode jn) throws IOException {
        if (!jn.isObject()) {
            throw new MarshallingException("Json does not contain an object as expected");
        }
        JsonNode entityNode = jn.get(ENTITY_FIELD_NAME);
        if (entityNode == null || entityNode.isNull() || !entityNode.isTextual()) {
            throw new MarshallingException("Json object does not contain an entity-type field as expected");
        }
        String entityValue = entityNode.textValue();
        if (!entityType.equals(entityValue)) {
            throw new MarshallingException("Json object entity-type is wrong. Expected is " + entityType + " but was "
                    + entityValue);
        }
        return readEntity(jn);
    }

    /**
     * Implement this method to read the entity.
     *
     * @param jn A {@link JsonNode} pointing at the root of the json input.
     * @return The parsed entity.
     * @since 7.2
     */
    protected abstract EntityType readEntity(JsonNode jn) throws IOException;

}
