/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs.spi;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.DeserializationProblemHandler;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonCachable;
import org.codehaus.jackson.map.deser.BeanDeserializer;
import org.codehaus.jackson.map.deser.BeanDeserializerModifier;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.map.type.TypeBindings;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.map.type.TypeModifier;
import org.codehaus.jackson.type.JavaType;
import org.nuxeo.ecm.automation.client.Constants;
import org.nuxeo.ecm.automation.client.OperationRequest;
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

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JsonMarshalling {

    /**
     *
     * @author matic
     * @since 5.5
     */
    public static class ThowrableTypeModifier extends TypeModifier {
        @Override
        public JavaType modifyType(JavaType type, Type jdkType,
                TypeBindings context, TypeFactory typeFactory) {
            Class<?> raw = type.getRawClass();
            if (raw.isAssignableFrom(Throwable.class)) {
                return typeFactory.constructType(RemoteThrowable.class);
            }
            return type;
        }
    }

    public static class RemoteThrowable extends Throwable {

        private static final long serialVersionUID = 1L;

        protected RemoteThrowable(String message) {
            super(message);
        }

        protected final HashMap<String, JsonNode> otherNodes = new HashMap<String, JsonNode>();

        public Map<String, JsonNode> getOtherNodes() {
            return Collections.unmodifiableMap(otherNodes);
        }
    }

    @JsonCachable(false)
    public static class ThrowableDeserializer extends
            org.codehaus.jackson.map.deser.ThrowableDeserializer {

        protected HashMap<String, JsonNode> otherNodes = new HashMap<String, JsonNode>();

        public ThrowableDeserializer(BeanDeserializer src) {
            super(src);
        }

        @Override
        public Object deserializeFromObject(JsonParser jp,
                DeserializationContext ctxt) throws IOException,
                JsonProcessingException {

            RemoteThrowable t = (RemoteThrowable) super.deserializeFromObject(
                    jp, ctxt);
            t.otherNodes.putAll(otherNodes);
            return t;
        }
    }

    private JsonMarshalling() {
    }

    protected static JsonFactory factory = newJsonFactory();

    protected static final HashMap<String, JsonMarshaller<?>> marshallersByType = new HashMap<String, JsonMarshaller<?>>();

    protected static final HashMap<Class<?>, JsonMarshaller<?>> marshallersByJavaType = new HashMap<Class<?>, JsonMarshaller<?>>();

    public static JsonFactory getFactory() {
        return factory;
    }

    public static JsonFactory newJsonFactory() {
        JsonFactory jf = new JsonFactory();
        ObjectMapper oc = new ObjectMapper(jf);
        final TypeFactory typeFactoryWithModifier = oc.getTypeFactory().withModifier(
                new ThowrableTypeModifier());
        oc.setTypeFactory(typeFactoryWithModifier);
        oc.getDeserializationConfig().addHandler(
                new DeserializationProblemHandler() {
                    @Override
                    public boolean handleUnknownProperty(
                            DeserializationContext ctxt,
                            JsonDeserializer<?> deserializer,
                            Object beanOrClass, String propertyName)
                            throws IOException, JsonProcessingException {
                        if (deserializer instanceof ThrowableDeserializer) {
                            JsonParser jp = ctxt.getParser();
                            JsonNode propertyNode = jp.readValueAsTree();
                            ((ThrowableDeserializer) deserializer).otherNodes.put(
                                    propertyName, propertyNode);
                            return true;
                        }
                        return false;
                    }
                });
        final SimpleModule module = new SimpleModule("automation",
                Version.unknownVersion()) {

            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);

                context.addBeanDeserializerModifier(new BeanDeserializerModifier() {

                    @Override
                    public JsonDeserializer<?> modifyDeserializer(
                            DeserializationConfig config,
                            BasicBeanDescription beanDesc,
                            JsonDeserializer<?> deserializer) {
                        if (!Throwable.class.isAssignableFrom(beanDesc.getBeanClass())) {
                            return super.modifyDeserializer(config, beanDesc,
                                    deserializer);
                        }
                        return new ThrowableDeserializer(
                                (BeanDeserializer) deserializer);
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

    public static OperationRegistry readRegistry(String content)
            throws Exception {
        HashMap<String, OperationDocumentation> ops = new HashMap<String, OperationDocumentation>();
        HashMap<String, OperationDocumentation> chains = new HashMap<String, OperationDocumentation>();
        HashMap<String, String> paths = new HashMap<String, String>();

        JsonParser jp = factory.createJsonParser(content);
        jp.nextToken(); // start_obj
        JsonToken tok = jp.nextToken();
        while (tok != JsonToken.END_OBJECT) {
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
        return new OperationRegistry(paths, ops, chains);
    }

    private static void readOperations(JsonParser jp,
            Map<String, OperationDocumentation> ops) throws Exception {
        jp.nextToken(); // skip [
        JsonToken tok = jp.nextToken();
        while (tok != JsonToken.END_ARRAY) {
            OperationDocumentation op = JsonOperationMarshaller.read(jp);
            ops.put(op.id, op);
            tok = jp.nextToken();
        }
    }

    private static void readChains(JsonParser jp,
            Map<String, OperationDocumentation> chains) throws Exception {
        jp.nextToken(); // skip [
        JsonToken tok = jp.nextToken();
        while (tok != JsonToken.END_ARRAY) {
            OperationDocumentation op = JsonOperationMarshaller.read(jp);
            chains.put(op.id, op);
            tok = jp.nextToken();
        }
    }

    private static void readPaths(JsonParser jp, Map<String, String> paths)
            throws Exception {
        jp.nextToken(); // skip {
        JsonToken tok = jp.nextToken();
        while (tok != JsonToken.END_OBJECT) {
            jp.nextToken();
            paths.put(jp.getCurrentName(), jp.getText());
            tok = jp.nextToken();
        }
    }

    public static Object readEntity(String content) throws Exception {
        if (content.length() == 0) { // void response
            return null;
        }
        JsonParser jp = factory.createJsonParser(content);
        jp.nextToken(); // will return JsonToken.START_OBJECT (verify?)
        jp.nextToken();
        if (!Constants.KEY_ENTITY_TYPE.equals(jp.getText())) {
            throw new RuntimeException(
                    "unuspported respone type. No entity-type key found at top of the object");
        }
        jp.nextToken();
        String etype = jp.getText();
        JsonMarshaller<?> jm = marshallersByType.get(etype);
        if (jm == null) {
            // fall-back on generic java class loading in case etype matches a
            // valid class name
            try {
                Class<?> loadClass = Thread.currentThread().getContextClassLoader().loadClass(
                        etype);
                ObjectMapper mapper = new ObjectMapper();
                jp.nextToken(); // move to next field
                jp.nextToken(); // value field name
                jp.nextToken(); // value field content
                return mapper.readValue(jp, loadClass);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("No marshaller for " + etype
                        + " and not a valid Java class name either.", e);
            }
        }
        return jm.read(jp);
    }

    public static String writeRequest(OperationRequest req) throws Exception {
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

    public static void writeMap(JsonGenerator jg, Map<String, Object> map)
            throws Exception {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object obj = entry.getValue();
            if (obj instanceof String) {
                jg.writeStringField(entry.getKey(), (String) obj);
            } else if (obj instanceof PropertyMap || obj instanceof OperationInput) {
                jg.writeStringField(entry.getKey(), obj.toString());
            } else {
                jg.writeFieldName(entry.getKey());
                jg.writeObject(obj);
            }
        }
    }

}
