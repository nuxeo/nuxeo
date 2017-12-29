/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.automation.client.jaxrs.util;

import java.io.IOException;
import java.util.ArrayList;

import org.nuxeo.ecm.automation.client.model.OperationDocumentation;
import org.nuxeo.ecm.automation.client.model.OperationDocumentation.Param;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JsonOperationMarshaller {

    public static OperationDocumentation read(JsonParser jp) throws IOException {
        OperationDocumentation op = new OperationDocumentation();
        JsonToken tok = jp.nextToken(); // skip {
        while (tok != null && tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            jp.nextToken();
            if ("id".equals(key)) {
                op.id = jp.getText();
            } else if ("aliases".equals(key)) {
                op.aliases = readStringArray(jp);
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
            throw new IllegalArgumentException("Unexpected end of stream.");
        }
        return op;
    }

    public static String[] readStringArray(JsonParser jp) throws IOException {
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

    private static void readParams(JsonParser jp, OperationDocumentation op) throws IOException {
        JsonToken tok = jp.nextToken(); // skip [
        if (tok == JsonToken.END_ARRAY) {
            return;
        }
        do {
            readParam(jp, op);
            tok = jp.nextToken();
        } while (tok != JsonToken.END_ARRAY);
    }

    private static void readParam(JsonParser jp, OperationDocumentation op) throws IOException {
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

    public static void write(JsonParser jp, OperationDocumentation op) {

    }

}
