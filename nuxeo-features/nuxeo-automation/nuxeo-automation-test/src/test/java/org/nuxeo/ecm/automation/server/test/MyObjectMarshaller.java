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
package org.nuxeo.ecm.automation.server.test;

import java.io.IOException;

import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * TODO must use ObjectCodec on client too.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class MyObjectMarshaller implements JsonMarshaller<MyObject> {

    @Override
    public Class<MyObject> getJavaType() {
        return MyObject.class;
    }

    @Override
    public String getType() {
        return "msg";
    }

    @Override
    public MyObject read(JsonParser jp) throws IOException {
        if (jp.getCodec() == null) {
            jp.setCodec(new ObjectMapper());
        }
        jp.nextValue();
        return jp.readValueAs(MyObject.class);
    }

    public void write(JsonGenerator jg, Object value) throws IOException {
        throw new UnsupportedOperationException();
    }

}
