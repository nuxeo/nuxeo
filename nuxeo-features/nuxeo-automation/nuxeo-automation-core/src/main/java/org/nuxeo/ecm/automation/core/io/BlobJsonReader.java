package org.nuxeo.ecm.automation.core.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.nuxeo.ecm.automation.core.util.ComplexTypeJSONDecoder;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

@Setup(mode = SINGLETON, priority = REFERENCE)
public class BlobJsonReader extends AbstractJsonReader<Blob> {

    @Override
    public Blob read(JsonNode jn) throws IOException {
        if (jn.isObject()) {
            return ComplexTypeJSONDecoder.getBlobFromJSON((ObjectNode) jn);
        }
        return null;
    }

}
