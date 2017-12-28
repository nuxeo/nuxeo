/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat
 */
package org.nuxeo.ecm.automation.server.test.json;

import java.io.IOException;

import org.nuxeo.ecm.automation.io.services.codec.ObjectCodec;
import org.nuxeo.ecm.automation.server.test.json.JSONOperationWithArrays.SimplePojo;
import org.nuxeo.ecm.core.api.CoreSession;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

public class SimplePojoObjectCodec extends ObjectCodec<SimplePojo> {

    public SimplePojoObjectCodec() {
        super(SimplePojo.class);
    }

    @Override
    public String getType() {
        return "simplePojo";
    }

    @Override
    public void write(JsonGenerator jg, SimplePojo value) throws IOException {
        jg.writeObject(value);
    }

    @Override
    public SimplePojo read(JsonParser jp, CoreSession session) throws IOException {
        return jp.readValueAs(SimplePojo.class);
    }

    @Override
    public boolean isBuiltin() {
        return false;
    }

}