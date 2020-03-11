/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.api.validation;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;

/**
 * Provides a way to validate {@link DocumentModel} according to their {@link Schema}'s constraints.
 *
 * @since 7.1
 */
public interface DocumentValidationService {

    String CTX_MAP_KEY = "DocumentValidationService.Forcing";

    String CTX_CREATEDOC = "createDocument";

    String CTX_SAVEDOC = "saveDocument";

    String CTX_IMPORTDOC = "importDocument";

    public enum Forcing {
        TURN_ON, TURN_OFF, USUAL;
    }

    /**
     * To activate validation in some context, for example "CoreSession.saveDocument", you have to contribute to
     * component "org.nuxeo.ecm.core.api.DocumentValidationService" with extension point "activations".
     * <p>
     * Example :
     * </p>
     *
     * <pre>
     * {@code
     * <extension target="org.nuxeo.ecm.core.api.DocumentValidationService" point="activations">
     *   <validation context="CoreSession.saveDocument" activated="false" />
     * </extension>
     * }
     * </pre>
     * <p>
     * Here are some available context :
     * </p>
     * <ul>
     * <li>CoreSession.createDocument</li>
     * <li>CoreSession.saveDocument</li>
     * <li>CoreSession.importDocument</li>
     * </ul>
     *
     * @param context A string representation of the context, where validation service should be activated.
     * @param contextMap if not null, search forcing flag in the context map. see {@link Forcing} for values and
     *            {@link #CTX_MAP_KEY} for the key.
     * @return true if validation is activated in the specified context, false otherwise.
     * @since 7.1
     */
    boolean isActivated(String context, Map<String, Serializable> contextMap);

    /**
     * Validates all {@link Field} of all {@link Schema} of a {@link DocumentModel}. Including no dirty properties.
     *
     * @since 7.1
     */
    DocumentValidationReport validate(DocumentModel document);

    /**
     * Validates all {@link Field} of all {@link Schema} of a {@link DocumentModel}.
     *
     * @param dirtyOnly If true, limit validation to dirty properties of the {@link DocumentModel}.
     * @since 7.1
     */
    DocumentValidationReport validate(DocumentModel document, boolean dirtyOnly);

    /**
     * Validates a value according to some {@link Field} definition.
     *
     * @since 7.1
     */
    DocumentValidationReport validate(Field field, Object value);

    /**
     * Validates a value according to some {@link Field} definition.
     *
     * @param validateSubProperties Tell whether the sub properties must be validated.
     * @since 7.2
     */
    DocumentValidationReport validate(Field field, Object value, boolean validateSubProperties);

    /**
     * Validates a property according to its {@link Field} definition.
     *
     * @since 7.1
     */
    DocumentValidationReport validate(Property property);

    /**
     * Validates a property according to its {@link Field} definition.
     *
     * @param validateSubProperties Tell whether the sub properties must be validated.
     * @since 7.10
     */
    DocumentValidationReport validate(Property property, boolean validateSubProperties);

    /**
     * Validates a value according to some {@link Field} definition.
     *
     * @param xpath schema:fieldName, for example dc:title - the xpath could also be a value that match a complex
     *            property field (for example, an field of a complex type in a list: schema:list:complex:field).
     * @throws IllegalArgumentException If the xpath does not match any field.
     * @since 7.1
     */
    DocumentValidationReport validate(String xpath, Object value);

    /**
     * Validates a value according to some {@link Field} definition.
     *
     * @param xpath schema:fieldName, for example dc:title - the xpath could also be a value that match a complex
     *            property field (for example, an field of a complex type in a list: schema:list:complex:field).
     * @param validateSubProperties Tell whether the sub properties must be validated.
     * @throws IllegalArgumentException If the xpath does not match any field.
     * @since 7.10
     */
    DocumentValidationReport validate(String xpath, Object value, boolean validateSubProperties);

}
