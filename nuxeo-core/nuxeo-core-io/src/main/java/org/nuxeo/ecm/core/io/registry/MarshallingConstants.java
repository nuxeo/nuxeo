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
     * Parameter name to specify how deep the marshallers should call sub marshallers.
     */
    String MAX_DEPTH_PARAM = "depth";

    /**
     * Value to specify embed all or fetch all
     */
    String WILDCARD_VALUE = "*";

    /**
     * @deprecated use {@value #EMBED_PARAM} concatenated with {@link #EMBED_PROPERTIES} (example:
     *             embed:props=dublincore)
     */
    @Deprecated
    String DOCUMENT_PROPERTIES_HEADER = "X-NXDocumentProperties";

    /**
     * @deprecated use {@value #EMBED_PARAM} concatenated with {@link #EMBED_ENRICHERS} (example: embed:adds=acls)
     */
    @Deprecated
    String NXCONTENT_CATEGORY_HEADER = "X-NXContext-Category";

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
