/*
 * Copyright (c) 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo
 */
package org.nuxeo.ecm.automation.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper to handle marshalling of complex types into a JSON-encoded string.
 *
 * @since 7.1
 */
public class ComplexPropertyJSONEncoder {

    private static JsonFactory getFactory() {
        JsonFactoryManager jsonFactoryManager = Framework.getLocalService(JsonFactoryManager.class);
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
