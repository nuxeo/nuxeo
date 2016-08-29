package org.nuxeo.ecm.directory.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.directory.Directory;

/**
 * @since 8.4
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DirectoryJsonWriter extends ExtensibleEntityJsonWriter<Directory> {

    public static final String ENTITY_TYPE = "directory";

    public DirectoryJsonWriter() {
        super(ENTITY_TYPE, Directory.class);
    }

    @Override
    protected void writeEntityBody(Directory entity, JsonGenerator jg) throws IOException {
        jg.writeStringField("name", entity.getName());
    }

}