/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @author Anahide Tchertchian
 * @since 5.4
 * @since 10.1 converted to a marshaller
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class WidgetTypeDefinitionJsonWriter extends AbstractJsonWriter<WidgetTypeDefinition> {

    @Override
    public void write(WidgetTypeDefinition entity, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("name", entity.getName());

        List<String> aliases = entity.getAliases();
        if (CollectionUtils.isNotEmpty(aliases)) {
            writeSerializableListField("aliases", aliases, jg);
        }
        jg.writeStringField("handlerClassName", entity.getHandlerClassName());

        Map<String, String> properties = entity.getProperties();
        if (MapUtils.isNotEmpty(properties)) {
            // sort so that order is deterministic
            writeSerializableMapField("properties", new TreeMap<>(properties), jg);
        }

        WidgetTypeConfiguration configuration = entity.getConfiguration();
        if (configuration != null) {
            writeEntityField("configuration", configuration, jg);
        }
        jg.writeEndObject();
    }

}
