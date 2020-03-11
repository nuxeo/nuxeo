/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.export;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeDefinition;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 6.0
 * @since 10.1 converted to a marshaller
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class LayoutTypeDefinitionJsonWriter extends AbstractJsonWriter<LayoutTypeDefinition> {

    @Override
    public void write(LayoutTypeDefinition entity, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("name", entity.getName());

        List<String> aliases = entity.getAliases();
        if (CollectionUtils.isNotEmpty(aliases)) {
            writeSerializableListField("aliases", aliases, jg);
        }

        Map<String, String> templates = entity.getTemplates();
        if (MapUtils.isNotEmpty(templates)) {
            writeSerializableMapField("templates", new TreeMap<>(templates), jg);
        }

        LayoutTypeConfiguration configuration = entity.getConfiguration();
        if (configuration != null) {
            writeEntityField("configuration", configuration, jg);
        }
        jg.writeEndObject();
    }

}
