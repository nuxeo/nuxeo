/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.ecm.automation.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Helper to handle marshalling of complex types into a JSON-encoded string.
 *
 * @since 7.1
 */
public class ComplexPropertyJSONEncoder {

    private static JsonFactory getFactory() {
        JsonFactoryManager jsonFactoryManager = Framework.getService(JsonFactoryManager.class);
        return jsonFactoryManager.getJsonFactory();
    }

    public static String encode(ComplexProperty cp) throws IOException {
        return encode(cp, DateTimeFormat.W3C);
    }

    public static String encode(ComplexProperty cp, DateTimeFormat dateTimeFormat) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = getFactory().createJsonGenerator(out);
        JSONPropertyWriter.writePropertyValue(jg, cp, dateTimeFormat, null);
        jg.flush();
        jg.close();
        return out.toString("UTF-8");
    }
}
