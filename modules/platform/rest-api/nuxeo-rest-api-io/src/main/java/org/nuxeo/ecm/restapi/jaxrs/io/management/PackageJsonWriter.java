/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.restapi.jaxrs.io.management;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.nuxeo.connect.update.Package;
import org.nuxeo.ecm.core.io.marshallers.json.DefaultListJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 2025.14
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class PackageJsonWriter extends ExtensibleEntityJsonWriter<Package> {

    public static final String ENTITY_TYPE = "package";

    public PackageJsonWriter() {
        super(ENTITY_TYPE);
    }

    @Override
    protected void writeEntityBody(Package entity, JsonGenerator jg) throws IOException {
        writeStringFieldIfNotNull("id", entity.getId(), jg);
        writeStringFieldIfNotNull("name", entity.getName(), jg);
        writeStringFieldIfNotNull("title", entity.getTitle(), jg);
        writeStringFieldIfNotNull("description", entity.getDescription(), jg);
        if (entity.getVersion() != null) {
            jg.writeStringField("version", entity.getVersion().toString());
        }
        writeObjectFieldIfNotNull("type", entity.getType(), jg);
        writeObjectFieldIfNotNull("state", entity.getPackageState(), jg);
        writeObjectFieldIfNotNull("targetPlatforms", entity.getTargetPlatforms(), jg);
        writeStringFieldIfNotNull("vendor", entity.getVendor(), jg);
        jg.writeBooleanField("supportsHotReload", entity.supportsHotReload());
        writeEntityArrayField("provides", entity.getProvides(), jg);
        writeEntityArrayField("dependencies", entity.getDependencies(), jg);
        writeEntityArrayField("conflicts", entity.getConflicts(), jg);
        writeStringFieldIfNotNull("licenseType", entity.getLicenseType(), jg);
        writeStringFieldIfNotNull("licenseUrl", entity.getLicenseUrl(), jg);
    }

    @Setup(mode = SINGLETON, priority = REFERENCE)
    public static class ListJsonWriter extends DefaultListJsonWriter<Package> {

        public static final String ENTITY_TYPE = "packages";

        public ListJsonWriter() {
            super(ENTITY_TYPE);
        }
    }
}
