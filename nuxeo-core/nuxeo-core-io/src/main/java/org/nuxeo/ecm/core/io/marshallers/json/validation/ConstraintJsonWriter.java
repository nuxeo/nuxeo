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

package org.nuxeo.ecm.core.io.marshallers.json.validation;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint.Description;

import com.thoughtworks.xstream.io.json.JsonWriter;

/**
 * Convert {@link Constraint} to Json.
 * <p>
 * This marshaller is enrichable: register class implementing {@link AbstractJsonEnricher} and managing
 * {@link Constraint}.
 * </p>
 * <p>
 * This marshaller is also extensible: extend it and simply override
 * {@link ExtensibleEntityJsonWriter#extend(Constraint, JsonWriter)}.
 * </p>
 * <p>
 * Format is:
 *
 * <pre>
 * {@code
 * {
 *   "entity-type":"validation_constraint",
 *   "name": "CONSTRAINT_NAME",
 *   "parameters": {
 *     "PARAMETER1_NAME": "PARAMETER1_VALUE",
 *     "PARAMETER2_NAME": "PARAMETER2_VALUE",
 *     ...
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
public class ConstraintJsonWriter extends ExtensibleEntityJsonWriter<Constraint> {

    public static final String ENTITY_TYPE = "validation_constraint";

    public ConstraintJsonWriter() {
        super(ENTITY_TYPE, Constraint.class);
    }

    @Override
    protected void writeEntityBody(Constraint constraint, JsonGenerator jg) throws IOException {
        Description description = constraint.getDescription();
        jg.writeStringField("name", description.getName());
        // constraint parameters
        jg.writeObjectFieldStart("parameters");
        for (Map.Entry<String, Serializable> param : description.getParameters().entrySet()) {
            jg.writeStringField(param.getKey(), param.getValue().toString());
        }
        jg.writeEndObject();
    }

}