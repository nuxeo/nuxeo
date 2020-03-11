/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.io.registry;

import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.context.WrappedContext;

/**
 * This class gather all marshalling related constants.
 *
 * @since 7.2
 */
public interface MarshallingConstants {

    // Entity names

    /**
     * The field name for entity's type.
     */
    String ENTITY_FIELD_NAME = "entity-type";

    // Parameters

    /**
     * Prefix to put Nuxeo parameters in Headers.
     */
    String HEADER_PREFIX = "X-NX";

    /**
     * Parameter name to specify which document's properties should be loaded.
     */
    String EMBED_PROPERTIES = "properties";

    /**
     * Parameter name to specify which ContentEnrichers should be activated.
     */
    String EMBED_ENRICHERS = "enrichers";

    /**
     * Parameter name to specify which properties should be fetched.
     */
    String FETCH_PROPERTIES = "fetch";

    /**
     * Parameter name to specify which properties should be translated.
     */
    String TRANSLATE_PROPERTIES = "translate";

    /**
     * Default separator for enrichers, fetch and translate based properties.
     */
    char SEPARATOR = '-';

    /**
     * Parameter name to specify how deep the marshallers should call sub marshallers.
     */
    String MAX_DEPTH_PARAM = "depth";

    /**
     * Value to specify embed all or fetch all
     */
    String WILDCARD_VALUE = "*";

    // Technical

    /**
     * Context parameter key used to get current {@link WrappedContext} in a {@link RenderingContext}.
     */
    String WRAPPED_CONTEXT = "_MarshalledEntitiesWrappedContext";

    /**
     * Key used to isolate depth control key: counter to manage infinite marshaller to marshaller call.
     */
    String DEPTH_CONTROL_KEY_PREFIX = "_DepthControlKey_";

}
