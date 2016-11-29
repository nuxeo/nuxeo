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
 *     bstefanescu
 *     vpasquier
 */
package org.nuxeo.ecm.webengine.app;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 6.0
 */
public class JsonWebengineWriter {

    static JsonFactoryManager jsonFactoryManager;

    private static JsonFactory getFactory() {
        jsonFactoryManager = Framework.getLocalService(JsonFactoryManager.class);
        return jsonFactoryManager.getJsonFactory();
    }

    private static JsonGenerator createGenerator(OutputStream out) throws IOException {
        return getFactory().createJsonGenerator(out, JsonEncoding.UTF8);
    }

    public static void writeException(OutputStream out, WebException webException, MediaType mediaType)
            throws IOException {
        writeException(createGenerator(out), webException, mediaType);
    }

    public static void writeException(JsonGenerator jg, WebException webException, MediaType mediaType)
            throws IOException {
        jg.writeStartObject();
        jg.writeStringField("entity-type", "exception");
        jg.writeStringField("code", webException.getType());
        jg.writeNumberField("status", webException.getStatus());
        // jg.writeStringField("help_url", eh.getHelpUrl());
        // jg.writeStringField("request_id", eh.getRequestId());
        jg.writeStringField("message", webException.getMessage());
        if (jsonFactoryManager.isStackDisplay()
                || MediaType.valueOf(MediaType.APPLICATION_JSON + "+nxentity").equals(mediaType)) {
            jg.writeStringField("stacktrace", webException.getStackTraceString());
            jg.writeObjectField("exception", webException.getCause());
        }
        jg.writeEndObject();
        jg.flush();
    }

}
