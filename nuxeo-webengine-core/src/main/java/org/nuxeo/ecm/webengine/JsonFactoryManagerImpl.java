/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 *     Vladimir Pasquir <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.webengine;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.map.ser.BeanSerializer;
import org.codehaus.jackson.map.ser.BeanSerializerModifier;
import org.nuxeo.runtime.api.Framework;

import java.io.IOException;

/**
 * @since 5.9.6
 */
public class JsonFactoryManagerImpl implements JsonFactoryManager {

    public static final String REST_STACK_DISPLAY = "org.nuxeo.rest.stack" +
            ".enable";

    protected boolean stackDisplay;

    public JsonFactoryManagerImpl() {
        stackDisplay = Framework.isBooleanPropertyTrue(REST_STACK_DISPLAY);
    }

    private static class ThrowableSerializer extends BeanSerializer {

        public ThrowableSerializer(BeanSerializer src) {
            super(src);
        }

        @Override
        protected void serializeFields(Object bean, JsonGenerator jgen,
                SerializerProvider provider) throws IOException,
                JsonGenerationException {
            serializeClassName(bean, jgen, provider);
            super.serializeFields(bean, jgen, provider);
        }

        @Override
        protected void serializeFieldsFiltered(Object bean, JsonGenerator jgen,
                SerializerProvider provider) throws IOException,
                JsonGenerationException {
            serializeClassName(bean, jgen, provider);
            super.serializeFieldsFiltered(bean, jgen, provider);
        }

        protected void serializeClassName(Object bean, JsonGenerator jgen,
                SerializerProvider provider) throws JsonGenerationException,
                IOException {
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
        final SimpleModule module = new SimpleModule("webengine",
                Version.unknownVersion()) {

            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);

                context.addBeanSerializerModifier(new BeanSerializerModifier() {

                    @Override
                    public JsonSerializer<?> modifySerializer(
                            SerializationConfig config,
                            BasicBeanDescription beanDesc,
                            JsonSerializer<?> serializer) {
                        if (!Throwable.class.isAssignableFrom(beanDesc.getBeanClass())) {
                            return super.modifySerializer(config, beanDesc,
                                    serializer);
                        }
                        return new ThrowableSerializer(
                                (BeanSerializer) serializer);
                    }
                });
            }
        };
        oc.registerModule(module);
        oc.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
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
