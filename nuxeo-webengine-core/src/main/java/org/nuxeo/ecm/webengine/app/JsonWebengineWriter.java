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

import com.google.common.base.Objects;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.runtime.api.Framework;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @since 5.9.6
 */
public class JsonWebengineWriter {

    protected boolean stackEnable;

    public static final String REST_STACK_DISPLAY = "org.nuxeo.rest.stack.enable";

    public JsonWebengineWriter() {
        stackEnable = Boolean.valueOf(Objects.firstNonNull(Framework
                .getProperty(REST_STACK_DISPLAY), "false"));
    }

    private static JsonFactory getFactory() {
        return Framework.getLocalService(JsonFactoryManager.class)
                .getJsonFactory();
    }

    private static JsonGenerator createGenerator(OutputStream out)
            throws IOException {
        return getFactory().createJsonGenerator(out, JsonEncoding.UTF8);
    }

    public static void writeException(OutputStream out, WebException eh)
            throws IOException {
        writeException(createGenerator(out), eh);
    }

    public static void writeException(JsonGenerator jg, WebException eh)
            throws IOException {
        jg.writeStartObject();
        jg.writeStringField("entity-type", "exception");
        jg.writeStringField("code", eh.getType());
        jg.writeNumberField("status", eh.getStatus());
        //jg.writeStringField("help_url", eh.getHelpUrl());
        //jg.writeStringField("request_id", eh.getRequestId());
        jg.writeStringField("message", eh.getMessage());
        jg.writeObjectField("stacktrace", eh.getStackTraceString());
        jg.writeObjectField("exception", eh.getCause());
        jg.writeEndObject();
        jg.flush();
    }

}
