/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.jaxrs.io;

import java.io.IOException;
import java.io.OutputStream;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.runtime.api.Framework;

/**
 *
 *
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
     *
     */
    public static JsonGenerator createJsonGenerator(JsonFactory jsonFactory,
            OutputStream out) throws IOException {
        return jsonFactory.createJsonGenerator(out, JsonEncoding.UTF8);
    }

    /**
     * @param out
     * @return
     * @throws IOException
     *
     */
    public static JsonGenerator createJsonGenerator(OutputStream out) throws IOException {
        JsonFactory jsonFactory = Framework.getLocalService(JsonFactoryManager.class).getJsonFactory();
        return createJsonGenerator(jsonFactory, out);
    }

}
