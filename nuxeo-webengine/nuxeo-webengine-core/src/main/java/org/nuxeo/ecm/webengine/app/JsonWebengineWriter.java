/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *     vpasquier
 */
package org.nuxeo.ecm.webengine.app;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.runtime.api.Framework;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @since 6.0
 */
public class JsonWebengineWriter {

    static JsonFactoryManager jsonFactoryManager;

    private static JsonFactory getFactory() {
        jsonFactoryManager = Framework.getLocalService(JsonFactoryManager
                .class);
        return jsonFactoryManager.getJsonFactory();
    }

    private static JsonGenerator createGenerator(OutputStream out)
            throws IOException {
        return getFactory().createJsonGenerator(out, JsonEncoding.UTF8);
    }

    public static void writeException(OutputStream out,
            WebException webException, MediaType mediaType)
            throws IOException {
        writeException(createGenerator(out), webException, mediaType);
    }

    public static void writeException(JsonGenerator jg,
            WebException webException, MediaType mediaType)
            throws IOException {
        jg.writeStartObject();
        jg.writeStringField("entity-type", "exception");
        jg.writeStringField("code", webException.getType());
        jg.writeNumberField("status", webException.getStatus());
        //jg.writeStringField("help_url", eh.getHelpUrl());
        //jg.writeStringField("request_id", eh.getRequestId());
        jg.writeStringField("message", webException.getMessage());
        if (jsonFactoryManager.isStackDisplay() || MediaType.valueOf
                (MediaType.APPLICATION_JSON + "+nxentity").equals(mediaType)) {
            jg.writeStringField("stacktrace",
                    webException.getStackTraceString());
            jg.writeObjectField("exception", webException.getCause());
        }
        jg.writeEndObject();
        jg.flush();
    }

}
