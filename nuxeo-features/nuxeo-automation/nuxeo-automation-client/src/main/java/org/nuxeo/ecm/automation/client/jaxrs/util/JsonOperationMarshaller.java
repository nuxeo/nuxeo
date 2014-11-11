/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs.util;

import java.util.ArrayList;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.nuxeo.ecm.automation.client.model.OperationDocumentation;
import org.nuxeo.ecm.automation.client.model.OperationDocumentation.Param;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JsonOperationMarshaller {

    public static OperationDocumentation read(JsonParser jp) throws Exception {
        OperationDocumentation op = new OperationDocumentation();
        JsonToken tok = jp.nextToken(); // skip {
        while (tok != null && tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            jp.nextToken();
            if ("id".equals(key)) {
                op.id = jp.getText();
            } else if ("label".equals(key)) {
                op.label = jp.getText();
            } else if ("category".equals(key)) {
                op.category = jp.getText();
            } else if ("requires".equals(key)) {
                op.requires = jp.getText();
            } else if ("description".equals(key)) {
                op.description = jp.getText();
            } else if ("url".equals(key)) {
                op.url = jp.getText();
            } else if ("since".equals(key)) {
                op.since = jp.getText();
            } else if ("signature".equals(key)) {
                op.signature = readStringArray(jp);
            } else if ("params".equals(key)) {
                readParams(jp, op);
            } else {
                jp.skipChildren();
            }
            tok = jp.nextToken();
        }
        if (tok == null) {
            throw new IllegalArgumentException(
                    "Unexpected end of stream.");
        }
        return op;
    }

    public static String[] readStringArray(JsonParser jp) throws Exception {
        JsonToken tok = jp.nextToken(); // skip [
        if (tok == JsonToken.END_ARRAY) {
            return null;
        }
        ArrayList<String> list = new ArrayList<String>();
        do {
            list.add(jp.getText());
            tok = jp.nextToken();
        } while (tok != JsonToken.END_ARRAY);
        return list.toArray(new String[list.size()]);
    }

    private static void readParams(JsonParser jp, OperationDocumentation op) throws Exception {
        JsonToken tok = jp.nextToken();  // skip [
        if (tok == JsonToken.END_ARRAY) {
            return;
        }
        do {
            readParam(jp, op);
            tok = jp.nextToken();
        } while(tok != JsonToken.END_ARRAY);
    }

    private static void readParam(JsonParser jp, OperationDocumentation op) throws Exception {
        Param para = new Param();
        JsonToken tok = jp.nextToken(); // skip {
        while (tok != null && tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            jp.nextToken();
            if ("name".equals(key)) {
                para.name = jp.getText();
            } else if ("type".equals(key)) {
                para.type = jp.getText();
            } else if ("description".equals(key)) {
                para.description = jp.getText();
            } else if ("required".equals(key)) {
                para.isRequired = jp.getBooleanValue();
            } else if ("widget".equals(key)) {
                para.widget = jp.getText();
            } else if ("values".equals(key)) {
                para.values = readStringArray(jp);
            }
            tok = jp.nextToken();
        }
        op.params.add(para);
    }

    public static void write(JsonParser jp, OperationDocumentation op) throws Exception {

    }

}
