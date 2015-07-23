package org.nuxeo.ecm.automation.server.test.json;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;
import org.nuxeo.ecm.automation.server.test.json.JSONOperationWithArrays.SimplePojo;

public class SimplePojoObjectMarshaller implements JsonMarshaller<SimplePojo> {

    public SimplePojoObjectMarshaller() {
    }

    @Override
    public String getType() {
        return "simplePojo";
    }

    @Override
    public Class<SimplePojo> getJavaType() {
        return SimplePojo.class;
    }

    @Override
    public SimplePojo read(JsonParser jp) throws IOException {
        jp.nextValue();
        return jp.readValueAs(SimplePojo.class);
    }

    @Override
    public void write(JsonGenerator jg, Object value) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("entity-type", "simplePojo");
        jg.writeObjectField("value", value);
        jg.writeEndObject();
    }

}