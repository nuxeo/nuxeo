package org.nuxeo.ecm.automation.server.test;

import java.io.IOException;
import java.io.StringWriter;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.server.jaxrs.io.JsonWriter;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;

@Operation(id=TestDataCapsule.ID, category="Test")
public class TestDataCapsule {

    public static final String ID = "TestDataCapsule";
    

    @OperationMethod
    public Blob getDataCapsule() throws IOException {
        StringWriter writer = new StringWriter();
        JsonGenerator generator = JsonWriter.getFactory().createJsonGenerator(writer);
        generator.writeObject(new MyObject());
        writer.close();
        String json = writer.toString();
        Blob blob = new StringBlob(json, "application/json");
        blob.setFilename(ID);
        return blob;
    }
}
