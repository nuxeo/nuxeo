/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.jaxrs.io;

import java.io.IOException;
import java.io.OutputStream;

import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 5.7.3
 */
public final class JsonHelper {

    /**
     * Helper method to centralize the JsonEncoding to use
     *
     * @param jsonFactory
     * @param out
     * @return
     * @throws IOException
     */
    public static JsonGenerator createJsonGenerator(JsonFactory jsonFactory, OutputStream out) throws IOException {
        return jsonFactory.createJsonGenerator(out, JsonEncoding.UTF8);
    }

    /**
     * @param out
     * @return
     * @throws IOException
     */
    public static JsonGenerator createJsonGenerator(OutputStream out) throws IOException {
        JsonFactory jsonFactory = Framework.getService(JsonFactoryManager.class).getJsonFactory();
        return createJsonGenerator(jsonFactory, out);
    }

}
