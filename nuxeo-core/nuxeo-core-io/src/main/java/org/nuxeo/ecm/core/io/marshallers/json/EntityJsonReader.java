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

import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.ENTITY_FIELD_NAME;

import java.io.IOException;

import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.core.io.registry.MarshallingException;

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
        String entityValue = entityNode.getTextValue();
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
