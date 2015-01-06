/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.automation.core.util;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.core.api.model.impl.DocumentPartImpl;
import org.nuxeo.ecm.core.api.model.impl.PropertyFactory;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.FieldImpl;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.runtime.api.Framework;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Map;

/**
 * Helper to handle encoding of Complex types to a JSON encoded String
 *
 * @since 7.1
 */
public class ComplexPropertyJSONEncoder {

    static JsonFactoryManager jsonFactoryManager;

    private static JsonFactory getFactory() {
        jsonFactoryManager = Framework.getLocalService(JsonFactoryManager.class);
        return jsonFactoryManager.getJsonFactory();
    }

    private static JsonGenerator createGenerator(OutputStream out) throws IOException {
        return getFactory().createJsonGenerator(out, JsonEncoding.UTF8);
    }

    public static String encode(ComplexProperty cp) throws IOException {
        return encode(cp, DateTimeFormat.W3C);
    }

    public static String encode(ComplexProperty cp, DateTimeFormat dateTimeFormat) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = getFactory().createJsonGenerator(out);
        JSONPropertyWriter.writePropertyValue(jg, cp, dateTimeFormat, null);
        jg.flush();
        String result = out.toString("UTF-8");
        return result;
    }

}
