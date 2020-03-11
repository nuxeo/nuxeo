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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

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
