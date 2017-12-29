/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs.spi;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.client.Constants;
import org.nuxeo.ecm.automation.client.OperationRequest;
import org.nuxeo.ecm.automation.client.RemoteThrowable;
import org.nuxeo.ecm.automation.client.jaxrs.impl.AutomationClientActivator;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.BooleanMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.DateMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.DocumentMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.DocumentsMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.ExceptionMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.LoginMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.NumberMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.RecordSetMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers.StringMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.util.JsonOperationMarshaller;
import org.nuxeo.ecm.automation.client.model.OperationDocumentation;
import org.nuxeo.ecm.automation.client.model.OperationInput;
import org.nuxeo.ecm.automation.client.model.OperationRegistry;
import org.nuxeo.ecm.automation.client.model.PropertyMap;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.type.TypeModifier;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JsonMarshalling {

    protected static final Log log = LogFactory.getLog(JsonMarshalling.class);

    /**
     * @author matic
     * @since 5.5
     */
    public static class ThowrableTypeModifier extends TypeModifier {
        @Override
        public JavaType modifyType(JavaType type, Type jdkType, TypeBindings context, TypeFactory typeFactory) {
            Class<?> raw = type.getRawClass();
            if (raw.equals(Throwable.class)) {
                // Use SimpleType.construct (even though deprecated) instead of typeFactory.constructType
                // because otherwise we have an infinite recursion due to the fact that
                // RemoteThrowable's superclass is Throwable itself, and Jackson doesn't deal with this.
                return SimpleType.construct(RemoteThrowable.class);
            }
            return type;
        }
    }

    public static class ThrowableDeserializer extends com.fasterxml.jackson.databind.deser.std.ThrowableDeserializer {

        protected Stack<Map<String, JsonNode>> unknownStack = new Stack<>();

        public ThrowableDeserializer(BeanDeserializer src) {
            super(src);
        }

        @Override
        public Object deserializeFromObject(JsonParser jp, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            unknownStack.push(new HashMap<String, JsonNode>());
            try {
            RemoteThrowable t = (RemoteThrowable) super.deserializeFromObject(jp, ctxt);
            t.getOtherNodes().putAll(unknownStack.peek());
                return t;
            } finally {
                unknownStack.pop();
            }
        }
    }

    private JsonMarshalling() {
    }

    protected static JsonFactory factory = newJsonFactory();

    protected static final Map<String, JsonMarshaller<?>> marshallersByType = new ConcurrentHashMap<String, JsonMarshaller<?>>();

    protected static final Map<Class<?>, JsonMarshaller<?>> marshallersByJavaType = new ConcurrentHashMap<Class<?>, JsonMarshaller<?>>();

    public static JsonFactory getFactory() {
        return factory;
    }

    public static JsonFactory newJsonFactory() {
        JsonFactory jf = new JsonFactory();
        ObjectMapper oc = new ObjectMapper(jf);
        final TypeFactory typeFactoryWithModifier = oc.getTypeFactory().withModifier(new ThowrableTypeModifier());
        oc.setTypeFactory(typeFactoryWithModifier);
        oc.addHandler(new DeserializationProblemHandler() {
            @Override
            public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser jp,
                    JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName)
                    throws IOException, JsonProcessingException {
                if (deserializer instanceof ThrowableDeserializer) {
                    JsonNode propertyNode = jp.readValueAsTree();
                    ((ThrowableDeserializer) deserializer).unknownStack.peek().put(propertyName, propertyNode);
                    return true;
                }
                return false;
            }
        });
        final SimpleModule module = new SimpleModule("automation", Version.unknownVersion()) {

            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);

                context.addBeanDeserializerModifier(new BeanDeserializerModifier() {

                    @Override
                    public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config,
                            BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
                        if (!Throwable.class.isAssignableFrom(beanDesc.getBeanClass())) {
                            return super.modifyDeserializer(config, beanDesc, deserializer);
                        }
                        return new ThrowableDeserializer((BeanDeserializer) deserializer);
                    }
                });
            }
        };
        oc.registerModule(module);
        jf.setCodec(oc);
        return jf;
    }

    static {
        addMarshaller(new DocumentMarshaller());
        addMarshaller(new DocumentsMarshaller());
        addMarshaller(new ExceptionMarshaller());
        addMarshaller(new LoginMarshaller());
        addMarshaller(new RecordSetMarshaller());
        addMarshaller(new StringMarshaller());
        addMarshaller(new BooleanMarshaller());
        addMarshaller(new NumberMarshaller());
        addMarshaller(new DateMarshaller());
    }

    public static void addMarshaller(JsonMarshaller<?> marshaller) {
        marshallersByType.put(marshaller.getType(), marshaller);
        marshallersByJavaType.put(marshaller.getJavaType(), marshaller);
    }

    @SuppressWarnings("unchecked")
    public static <T> JsonMarshaller<T> getMarshaller(String type) {
        return (JsonMarshaller<T>) marshallersByType.get(type);
    }

    @SuppressWarnings("unchecked")
    public static <T> JsonMarshaller<T> getMarshaller(Class<T> clazz) {
        return (JsonMarshaller<T>) marshallersByJavaType.get(clazz);
    }

    public static OperationRegistry readRegistry(String content) throws IOException {
        HashMap<String, OperationDocumentation> ops = new HashMap<String, OperationDocumentation>();
        HashMap<String, OperationDocumentation> chains = new HashMap<String, OperationDocumentation>();
        HashMap<String, String> paths = new HashMap<String, String>();

        JsonParser jp = factory.createJsonParser(content);
        jp.nextToken(); // start_obj
        JsonToken tok = jp.nextToken();
        while (tok != null && tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            if ("operations".equals(key)) {
                readOperations(jp, ops);
            } else if ("chains".equals(key)) {
                readChains(jp, chains);
            } else if ("paths".equals(key)) {
                readPaths(jp, paths);
            }
            tok = jp.nextToken();
        }
        if (tok == null) {
            throw new IllegalArgumentException("Unexpected end of stream.");
        }
        return new OperationRegistry(paths, ops, chains);
    }

    private static void readOperations(JsonParser jp, Map<String, OperationDocumentation> ops) throws IOException {
        jp.nextToken(); // skip [
        JsonToken tok = jp.nextToken();
        while (tok != null && tok != JsonToken.END_ARRAY) {
            OperationDocumentation op = JsonOperationMarshaller.read(jp);
            ops.put(op.id, op);
            if (op.aliases != null) {
                for (String alias : op.aliases) {
                    ops.put(alias, op);
                }
            }
            tok = jp.nextToken();
        }
    }

    private static void readChains(JsonParser jp, Map<String, OperationDocumentation> chains) throws IOException {
        jp.nextToken(); // skip [
        JsonToken tok = jp.nextToken();
        while (tok != null && tok != JsonToken.END_ARRAY) {
            OperationDocumentation op = JsonOperationMarshaller.read(jp);
            chains.put(op.id, op);
            tok = jp.nextToken();
        }
    }

    private static void readPaths(JsonParser jp, Map<String, String> paths) throws IOException {
        jp.nextToken(); // skip {
        JsonToken tok = jp.nextToken();
        while (tok != null && tok != JsonToken.END_OBJECT) {
            jp.nextToken();
            paths.put(jp.getCurrentName(), jp.getText());
            tok = jp.nextToken();
        }
        if (tok == null) {
            throw new IllegalArgumentException("Unexpected end of stream.");
        }

    }

    public static Object readEntity(String content) throws IOException {
        if (content.length() == 0) { // void response
            return null;
        }
        JsonParser jp = factory.createJsonParser(content);
        jp.nextToken(); // will return JsonToken.START_OBJECT (verify?)
        jp.nextToken();
        if (!Constants.KEY_ENTITY_TYPE.equals(jp.getText())) {
            throw new RuntimeException("unuspported respone type. No entity-type key found at top of the object");
        }
        jp.nextToken();
        String etype = jp.getText();
        JsonMarshaller<?> jm = getMarshaller(etype);
        if (jm == null) {
            // fall-back on generic java class loading in case etype matches a
            // valid class name
            try {
                // Introspect bundle context to load marshalling class
                AutomationClientActivator automationClientActivator = AutomationClientActivator.getInstance();
                Class<?> loadClass;
                // Java mode or OSGi mode
                if (automationClientActivator == null) {
                    loadClass = Thread.currentThread().getContextClassLoader().loadClass(etype);
                } else {
                    loadClass = automationClientActivator.getContext().getBundle().loadClass(etype);
                }
                ObjectMapper mapper = new ObjectMapper();
                jp.nextToken(); // move to next field
                jp.nextToken(); // value field name
                jp.nextToken(); // value field content
                return mapper.readValue(jp, loadClass);
            } catch (ClassNotFoundException e) {
                log.warn("No marshaller for " + etype + " and not a valid Java class name either.");
                jp = factory.createJsonParser(content);
                return jp.readValueAsTree();
            }
        }
        return jm.read(jp);
    }

    public static String writeRequest(OperationRequest req) throws IOException {
        StringWriter writer = new StringWriter();
        Object input = req.getInput();
        JsonGenerator jg = factory.createJsonGenerator(writer);
        jg.writeStartObject();
        if (input instanceof OperationInput) {
            // Custom String serialization
            OperationInput operationInput = (OperationInput) input;
            String ref = operationInput.getInputRef();
            if (ref != null) {
                jg.writeStringField("input", ref);
            }
        } else if (input != null) {

            JsonMarshaller<?> marshaller = getMarshaller(input.getClass());
            if (marshaller != null) {
                // use the registered marshaller for this type
                jg.writeFieldName("input");
                marshaller.write(jg, input);
            } else {
                // fall-back to direct POJO to JSON mapping
                jg.writeObjectField("input", input);
            }
        }
        jg.writeObjectFieldStart("params");
        writeMap(jg, req.getParameters());
        jg.writeEndObject();
        jg.writeObjectFieldStart("context");
        writeMap(jg, req.getContextParameters());
        jg.writeEndObject();
        jg.writeEndObject();
        jg.close();
        return writer.toString();
    }

    public static void writeMap(JsonGenerator jg, Map<String, Object> map) throws IOException {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object param = entry.getValue();
            jg.writeFieldName(entry.getKey());
            write(jg, param);
        }
    }

    public static void write(JsonGenerator jg, Object obj) throws IOException {
        if (obj != null) {
            JsonMarshaller<?> marshaller = getMarshaller(obj.getClass());
            if (marshaller != null) {
                try {
                    marshaller.write(jg, obj);
                } catch (UnsupportedOperationException e) {
                    // Catch this exception to handle builtin marshaller exceptions
                    jg.writeObject(obj);
                }
            } else if (obj instanceof String) {
                jg.writeString((String) obj);
            } else if (obj instanceof PropertyMap || obj instanceof OperationInput) {
                jg.writeString(obj.toString());
            } else if (obj instanceof Iterable) {
                jg.writeStartArray();
                for (Object object : (Iterable) obj) {
                    write(jg, object);
                }
                jg.writeEndArray();
            } else if (obj.getClass().isArray()) {
                jg.writeStartArray();
                for (Object object : (Object[]) obj) {
                    write(jg, object);
                }
                jg.writeEndArray();
            } else {
                jg.writeObject(obj);
            }
        } else {
            jg.writeNull();
        }
    }

}
