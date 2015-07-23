package org.nuxeo.ecm.automation.server.test.json;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.nuxeo.ecm.automation.io.services.codec.ObjectCodec;
import org.nuxeo.ecm.automation.server.test.json.JSONOperationWithArrays.SimplePojo;
import org.nuxeo.ecm.core.api.CoreSession;

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