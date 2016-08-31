/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <grenard@nuxeo.com>
 */
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
        jg.writeStringField("schema", entity.getSchema());
        jg.writeStringField("idField", entity.getIdField());
        jg.writeStringField("parent", entity.getParentDirectory());
    }

}