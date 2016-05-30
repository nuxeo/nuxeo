/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stephane Lacoin at Nuxeo (aka matic)
 */
package org.nuxeo.connect.tools.report.apidoc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.json.JsonObject;
import javax.json.JsonReaderFactory;

import org.nuxeo.apidoc.introspection.RuntimeSnapshot;
import org.nuxeo.connect.tools.report.Report;
import org.nuxeo.runtime.api.Framework;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

/**
 *
 *
 * @since 8.3
 */
public class APIDocReport implements Report {

    @Override
    public JsonObject snapshot() throws IOException {
        XStream stream = new XStream(new JettisonMappedXmlDriver());
        stream.setMode(XStream.XPATH_RELATIVE_REFERENCES);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            stream.toXML(new RuntimeSnapshot(), out);
            return Framework.getService(JsonReaderFactory.class).createReader(new ByteArrayInputStream(out.toByteArray())).readObject();
        }
    }

}
