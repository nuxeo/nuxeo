package org.nuxeo.ecm.directory.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import org.nuxeo.ecm.core.io.marshallers.json.DefaultListJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.directory.Directory;

/**
 * @since 8.4
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DirectoryListJsonWriter extends DefaultListJsonWriter<Directory> {

    public static final String ENTITY_TYPE = "directories";

    public DirectoryListJsonWriter() {
        super(ENTITY_TYPE, Directory.class);
    }


}