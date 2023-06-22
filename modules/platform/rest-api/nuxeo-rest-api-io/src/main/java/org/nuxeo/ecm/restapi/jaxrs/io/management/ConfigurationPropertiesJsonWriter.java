/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.restapi.jaxrs.io.management;

import static org.nuxeo.common.function.ThrowableBiConsumer.asBiConsumer;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;
import static org.nuxeo.log4j.Redactor.REDACTED_PLACE_HOLDER;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.nuxeo.common.Environment;
import org.nuxeo.common.codec.Crypto;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.log4j.Redactor;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 2023
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class ConfigurationPropertiesJsonWriter extends ExtensibleEntityJsonWriter<ConfigurationProperties> {

    public static final String ENTITY_TYPE = "configuration";

    protected Redactor redactor = new Redactor();

    protected static final List<String> SECRET_KEYS = List.of("password", "token", "secret", "metrics.datadog.apiKey",
            Environment.SERVER_STATUS_KEY);

    public ConfigurationPropertiesJsonWriter() {
        super(ENTITY_TYPE, ConfigurationProperties.class);
    }

    @Override
    public void writeEntityBody(ConfigurationProperties entity, JsonGenerator jg) throws IOException {
        writeProperties("configuredProperties", entity.configuredProperties(), jg);
        writeProperties("runtimeProperties", entity.runtimeProperties(), jg);
    }

    protected void writeProperties(String name, Properties properties, JsonGenerator jg) throws IOException {
        jg.writeObjectFieldStart(name);
        properties.forEach(
                asBiConsumer((k, v) -> jg.writeStringField(k.toString(), maskSensitive(k.toString(), v.toString()))));
        jg.writeEndObject();
    }

    protected String maskSensitive(String key, String value) {
        if (SECRET_KEYS.stream().map(String::toLowerCase).anyMatch(key.toLowerCase()::contains)
                || Crypto.isEncrypted(value)) {
            value = REDACTED_PLACE_HOLDER;
        } else {
            value = redactor.maskSensitive(value);
        }
        return value;
    }
}
