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
 *     dmetzler
 *     Vladimir Pasquir <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.webengine;

import java.io.IOException;

import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

/**
 * @since 6.0
 */
public class JsonFactoryManagerImpl implements JsonFactoryManager {

    public static final String REST_STACK_DISPLAY = "org.nuxeo.rest.stack" + ".enable";

    protected boolean stackDisplay;

    public JsonFactoryManagerImpl() {
        stackDisplay = Framework.isBooleanPropertyTrue(REST_STACK_DISPLAY);
    }

    private static class ThrowableSerializer extends BeanSerializer {

        public ThrowableSerializer(BeanSerializer src) {
            super(src);
        }

        @Override
        protected void serializeFields(Object bean, JsonGenerator jgen, SerializerProvider provider)
                throws IOException, JsonGenerationException {
            serializeClassName(bean, jgen, provider);
            super.serializeFields(bean, jgen, provider);
        }

        @Override
        protected void serializeFieldsFiltered(Object bean, JsonGenerator jgen, SerializerProvider provider)
                throws IOException, JsonGenerationException {
            serializeClassName(bean, jgen, provider);
            super.serializeFieldsFiltered(bean, jgen, provider);
        }

        protected void serializeClassName(Object bean, JsonGenerator jgen, SerializerProvider provider)
                throws JsonGenerationException, IOException {
            jgen.writeFieldName("className");
            jgen.writeString(bean.getClass().getName());
        }
    }

    private JsonFactory factory;

    @Override
    public JsonFactory getJsonFactory() {
        if (factory == null) {
            factory = createFactory();
        }
        return factory;
    }

    @Override
    public JsonFactory createFactory() {
        JsonFactory factory = new JsonFactory();
        final ObjectMapper oc = new ObjectMapper(factory);
        final SimpleModule module = new SimpleModule("webengine", Version.unknownVersion()) {

            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);

                context.addBeanSerializerModifier(new BeanSerializerModifier() {

                    @Override
                    public JsonSerializer<?> modifySerializer(SerializationConfig config,
                            BeanDescription beanDesc, JsonSerializer<?> serializer) {
                        if (!Throwable.class.isAssignableFrom(beanDesc.getBeanClass())) {
                            return super.modifySerializer(config, beanDesc, serializer);
                        }
                        return new ThrowableSerializer((BeanSerializer) serializer);
                    }
                });
            }
        };
        oc.registerModule(module);
        factory.setCodec(oc);
        return factory;
    }

    @Override
    public boolean toggleStackDisplay() {
        return stackDisplay = !stackDisplay;
    }

    public boolean isStackDisplay() {
        return stackDisplay;
    }
}
