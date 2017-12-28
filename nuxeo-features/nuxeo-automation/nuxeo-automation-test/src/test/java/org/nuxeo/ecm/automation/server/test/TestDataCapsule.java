/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */
package org.nuxeo.ecm.automation.server.test;

import java.io.IOException;
import java.io.StringWriter;

import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

@Operation(id = TestDataCapsule.ID, category = "Test")
public class TestDataCapsule {

    public static final String ID = "TestDataCapsule";

    @OperationMethod
    public Blob getDataCapsule() throws IOException {
        StringWriter writer = new StringWriter();
        JsonFactory jsonFactory = Framework.getService(JsonFactoryManager.class).getJsonFactory();
        JsonGenerator generator = jsonFactory.createJsonGenerator(writer);

        generator.writeObject(new MyObject());
        writer.close();
        String json = writer.toString();
        return Blobs.createBlob(json, "application/json", null, ID);
    }

}
