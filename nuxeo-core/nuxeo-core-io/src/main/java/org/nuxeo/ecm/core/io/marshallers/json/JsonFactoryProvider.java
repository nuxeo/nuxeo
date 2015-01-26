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

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Provides a {@link JsonFactory} with {@link ObjectMapper}.
 *
 * @since TODO
 */
public final class JsonFactoryProvider {

    private JsonFactoryProvider() {
    }

    /**
     * A factory with simple {@link ObjectMapper} integrated.
     */
    private static JsonFactory jsonFactory = null;

    /**
     * @return A {@link JsonFactory} with a simple {@link ObjectMapper}.
     * @since 7.2
     */
    public static JsonFactory get() {
        if (jsonFactory == null) {
            jsonFactory = new JsonFactory(new ObjectMapper());
        }
        return jsonFactory;
    }

}
