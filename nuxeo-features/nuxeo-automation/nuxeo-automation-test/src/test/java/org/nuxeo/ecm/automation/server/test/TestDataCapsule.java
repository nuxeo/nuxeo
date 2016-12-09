package org.nuxeo.ecm.automation.server.test;

import java.io.IOException;
import java.io.StringWriter;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.runtime.api.Framework;

@Operation(id = TestDataCapsule.ID, category = "Test")
public class TestDataCapsule {

    public static final String ID = "TestDataCapsule";

    @OperationMethod
    public Blob getDataCapsule() throws IOException {
        StringWriter writer = new StringWriter();
        JsonFactory jsonFactory = Framework.getLocalService(JsonFactoryManager.class).getJsonFactory();
        JsonGenerator generator = jsonFactory.createJsonGenerator(writer);

        generator.writeObject(new MyObject());
        writer.close();
        String json = writer.toString();
        Blob blob = new StringBlob(json, "application/json", null);
        blob.setFilename(ID);
        return blob;
    }
}
