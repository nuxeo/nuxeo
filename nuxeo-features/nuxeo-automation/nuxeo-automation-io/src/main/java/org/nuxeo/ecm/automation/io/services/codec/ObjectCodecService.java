/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     vpasquier
 *     slacoin
 */
package org.nuxeo.ecm.automation.io.services.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.operations.business.adapter.BusinessAdapter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterDescriptor;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterService;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ObjectCodecService {

    protected static final Log log = LogFactory.getLog(ObjectCodecService.class);

    protected Map<Class<?>, ObjectCodec<?>> codecs;

    protected Map<String, ObjectCodec<?>> codecsByName;

    protected Map<Class<?>, ObjectCodec<?>> _codecs;

    protected Map<String, ObjectCodec<?>> _codecsByName;

    private JsonFactory jsonFactory;

    public ObjectCodecService(JsonFactory jsonFactory) {
        this.jsonFactory = jsonFactory;
        codecs = new HashMap<Class<?>, ObjectCodec<?>>();
        codecsByName = new HashMap<String, ObjectCodec<?>>();
        init();
    }

    protected void init() {
        new StringCodec().register(this);
        new DateCodec().register(this);
        new CalendarCodec().register(this);
        new BooleanCodec().register(this);
        new NumberCodec().register(this);
    }

    public void postInit() {
        DocumentAdapterCodec.register(this, Framework.getService(DocumentAdapterService.class));
    }

    /**
     * Get all codecs.
     */
    public Collection<ObjectCodec<?>> getCodecs() {
        return codecs().values();
    }

    public synchronized void addCodec(ObjectCodec<?> codec) {
        codecs.put(codec.getJavaType(), codec);
        codecsByName.put(codec.getType(), codec);
        _codecs = null;
        _codecsByName = null;
    }

    public synchronized void removeCodec(String name) {
        ObjectCodec<?> codec = codecsByName.remove(name);
        if (codec != null) {
            codecs.remove(codec.getJavaType());
            _codecs = null;
            _codecsByName = null;
        }
    }

    public synchronized void removeCodec(Class<?> objectType) {
        ObjectCodec<?> codec = codecs.remove(objectType);
        if (codec != null) {
            codecsByName.remove(codec.getType());
            _codecs = null;
            _codecsByName = null;
        }
    }

    public ObjectCodec<?> getCodec(Class<?> objectType) {
        return codecs().get(objectType);
    }

    public ObjectCodec<?> getCodec(String name) {
        return codecsByName().get(name);
    }

    public Map<Class<?>, ObjectCodec<?>> codecs() {
        Map<Class<?>, ObjectCodec<?>> cache = _codecs;
        if (cache == null) {
            synchronized (this) {
                _codecs = new HashMap<Class<?>, ObjectCodec<?>>(codecs);
                cache = _codecs;
            }
        }
        return cache;
    }

    public Map<String, ObjectCodec<?>> codecsByName() {
        Map<String, ObjectCodec<?>> cache = _codecsByName;
        if (cache == null) {
            synchronized (this) {
                _codecsByName = new HashMap<String, ObjectCodec<?>>(codecsByName);
                cache = _codecsByName;
            }
        }
        return cache;
    }

    public String toString(Object object) throws IOException {
        return toString(object, false);
    }

    public String toString(Object object, boolean preetyPrint) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        write(baos, object, preetyPrint);
        return baos.toString("UTF-8");
    }

    public void write(OutputStream out, Object object) throws IOException {
        write(out, object, false);
    }

    public void write(OutputStream out, Object object, boolean prettyPint) throws IOException {

        JsonGenerator jg = jsonFactory.createJsonGenerator(out, JsonEncoding.UTF8);
        if (prettyPint) {
            jg.useDefaultPrettyPrinter();
        }
        write(jg, object);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void write(JsonGenerator jg, Object object) throws IOException {
        if (object == null) {
            jg.writeStartObject();
            jg.writeStringField("entity-type", "null");
            jg.writeFieldName("value");
            jg.writeNull();
            jg.writeEndObject();
        } else {
            Class<?> clazz = object.getClass();
            ObjectCodec<?> codec = getCodec(clazz);
            if (codec == null) {
                writeGenericObject(jg, clazz, object);
            } else {
                jg.writeStartObject();
                jg.writeStringField("entity-type", codec.getType());
                jg.writeFieldName("value");
                ((ObjectCodec) codec).write(jg, object);
                jg.writeEndObject();
            }
        }
        jg.flush();
    }

    public Object read(String json, CoreSession session) throws IOException, ClassNotFoundException {
        return read(json, null, session);
    }

    public Object read(String json, ClassLoader cl, CoreSession session) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(json.getBytes());
        return read(in, cl, session);
    }

    public Object read(InputStream in, CoreSession session) throws IOException, ClassNotFoundException {
        return read(in, null, session);
    }

    public Object read(InputStream in, ClassLoader cl, CoreSession session) throws IOException, ClassNotFoundException {
        try (JsonParser jp = jsonFactory.createParser(in)) {
            return read(jp, cl, session);
        }
    }

    public Object read(JsonParser jp, ClassLoader cl, CoreSession session) throws IOException, ClassNotFoundException {
        JsonToken tok = jp.getCurrentToken();
        if (tok == null) {
            tok = jp.nextToken();
        }
        if (tok == JsonToken.START_OBJECT) {
            tok = jp.nextToken();
        } else if (tok != JsonToken.FIELD_NAME) {
            throw new IllegalStateException(
                    "Invalid parser state. Current token must be either start_object or field_name");
        }
        String key = jp.getCurrentName();
        if (!"entity-type".equals(key)) {
            throw new IllegalStateException("Invalid parser state. Current field must be 'entity-type'");
        }
        jp.nextToken();
        String name = jp.getText();
        if (name == null) {
            throw new IllegalStateException("Invalid stream. Entity-Type is null");
        }
        jp.nextValue(); // move to next value
        ObjectCodec<?> codec = codecsByName.get(name);
        if (codec == null) {
            return readGenericObject(jp, name, cl);
        } else {
            return codec.read(jp, session);
        }
    }

    public Object readNode(JsonNode node, ClassLoader cl, CoreSession session) throws IOException {
        // Handle simple scalar types
        if (node.isNumber()) {
            return node.numberValue();
        } else if (node.isBoolean()) {
            return node.booleanValue();
        } else if (node.isTextual()) {
            return node.textValue();
        } else if (node.isArray()) {
            List<Object> result = new ArrayList<>();
            Iterator<JsonNode> elements = node.elements();
            while (elements.hasNext()) {
                result.add(readNode(elements.next(), cl, session));
            }
            return result;
        }
        JsonNode entityTypeNode = node.get("entity-type");
        JsonNode valueNode = node.get("value");
        if (entityTypeNode != null && entityTypeNode.isTextual()) {
            String type = entityTypeNode.textValue();
            ObjectCodec<?> codec = codecsByName.get(type);
            // handle structured entity with an explicit type declaration
            if (valueNode == null) {
                try (JsonParser jp = jsonFactory.createParser(node.toString())) {
                    if (codec == null) {
                        return readGenericObject(jp, type, cl);
                    } else {
                        return codec.read(jp, session);
                    }
                }
            }
            try (JsonParser valueParser = valueNode.traverse()) {
                if (valueParser.getCodec() == null) {
                    valueParser.setCodec(new ObjectMapper());
                }
                if (valueParser.getCurrentToken() == null) {
                    valueParser.nextToken();
                }
                if (codec == null) {
                    return readGenericObject(valueParser, type, cl);
                } else {
                    return codec.read(valueParser, session);
                }
            }
        }
        // fallback to returning the original json node
        return node;
    }

    public Object readNode(JsonNode node, CoreSession session) throws IOException {
        return readNode(node, null, session);
    }

    protected final void writeGenericObject(JsonGenerator jg, Class<?> clazz, Object object) throws IOException {
        jg.writeStartObject();
        if (clazz.isPrimitive()) {
            if (clazz == Boolean.TYPE) {
                jg.writeStringField("entity-type", "boolean");
                jg.writeBooleanField("value", (Boolean) object);
            } else if (clazz == Double.TYPE || clazz == Float.TYPE) {
                jg.writeStringField("entity-type", "number");
                jg.writeNumberField("value", ((Number) object).doubleValue());
            } else if (clazz == Integer.TYPE || clazz == Long.TYPE || clazz == Short.TYPE || clazz == Byte.TYPE) {
                jg.writeStringField("entity-type", "number");
                jg.writeNumberField("value", ((Number) object).longValue());
            } else if (clazz == Character.TYPE) {
                jg.writeStringField("entity-type", "string");
                jg.writeStringField("value", object.toString());
            }
            return;
        }
        if (jg.getCodec() == null) {
            jg.setCodec(new ObjectMapper());
        }
        if (object instanceof Iterable && clazz.getName().startsWith("java.")) {
            jg.writeStringField("entity-type", "list");
        } else if (object instanceof Map && clazz.getName().startsWith("java.")) {
            if (object instanceof LinkedHashMap) {
                jg.writeStringField("entity-type", "orderedMap");
            } else {
                jg.writeStringField("entity-type", "map");
            }
        } else {
            jg.writeStringField("entity-type", clazz.getName());
        }
        jg.writeObjectField("value", object);
        jg.writeEndObject();
    }

    protected final Object readGenericObject(JsonParser jp, String name, ClassLoader cl) throws IOException {
        if (jp.getCodec() == null) {
            jp.setCodec(new ObjectMapper());
        }
        if ("list".equals(name)) {
            return jp.readValueAs(ArrayList.class);
        } else if ("map".equals(name)) {
            return jp.readValueAs(HashMap.class);
        } else if ("orderedMap".equals(name)) {
            return jp.readValueAs(LinkedHashMap.class);
        }
        if (cl == null) {
            cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = ObjectCodecService.class.getClassLoader();
            }
        }
        Class<?> clazz;
        try {
            clazz = cl.loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
        return jp.readValueAs(clazz);
    }

    public static class StringCodec extends ObjectCodec<String> {
        public StringCodec() {
            super(String.class);
        }

        @Override
        public String getType() {
            return "string";
        }

        @Override
        public void write(JsonGenerator jg, String value) throws IOException {
            jg.writeString(value);
        }

        @Override
        public String read(JsonParser jp, CoreSession session) throws IOException {
            return jp.getText();
        }

        @Override
        public boolean isBuiltin() {
            return true;
        }

        public void register(ObjectCodecService service) {
            service.codecs.put(String.class, this);
            service.codecsByName.put(getType(), this);
        }
    }

    public static class DateCodec extends ObjectCodec<Date> {
        public DateCodec() {
            super(Date.class);
        }

        @Override
        public String getType() {
            return "date";
        }

        @Override
        public void write(JsonGenerator jg, Date value) throws IOException {
            jg.writeString(DateParser.formatW3CDateTime(value));
        }

        @Override
        public Date read(JsonParser jp, CoreSession session) throws IOException {
            return DateParser.parseW3CDateTime(jp.getText());
        }

        @Override
        public boolean isBuiltin() {
            return true;
        }

        public void register(ObjectCodecService service) {
            service.codecs.put(Date.class, this);
            service.codecsByName.put(getType(), this);
        }
    }

    public static class CalendarCodec extends ObjectCodec<Calendar> {
        public CalendarCodec() {
            super(Calendar.class);
        }

        @Override
        public String getType() {
            return "date";
        }

        @Override
        public void write(JsonGenerator jg, Calendar value) throws IOException {
            jg.writeString(DateParser.formatW3CDateTime(value.getTime()));
        }

        @Override
        public Calendar read(JsonParser jp, CoreSession session) throws IOException {
            Calendar c = Calendar.getInstance();
            c.setTime(DateParser.parseW3CDateTime(jp.getText()));
            return c;
        }

        @Override
        public boolean isBuiltin() {
            return true;
        }

        public void register(ObjectCodecService service) {
            service.codecs.put(Calendar.class, this);
        }
    }

    public static class BooleanCodec extends ObjectCodec<Boolean> {
        public BooleanCodec() {
            super(Boolean.class);
        }

        @Override
        public String getType() {
            return "boolean";
        }

        @Override
        public void write(JsonGenerator jg, Boolean value) throws IOException {
            jg.writeBoolean(value);
        }

        @Override
        public Boolean read(JsonParser jp, CoreSession session) throws IOException {
            return jp.getBooleanValue();
        }

        @Override
        public boolean isBuiltin() {
            return true;
        }

        public void register(ObjectCodecService service) {
            service.codecs.put(Boolean.class, this);
            service.codecs.put(Boolean.TYPE, this);
            service.codecsByName.put(getType(), this);
        }
    }

    public static class NumberCodec extends ObjectCodec<Number> {
        public NumberCodec() {
            super(Number.class);
        }

        @Override
        public String getType() {
            return "number";
        }

        @Override
        public void write(JsonGenerator jg, Number value) throws IOException {
            Class<?> cl = value.getClass();
            if (cl == Double.class || cl == Float.class) {
                jg.writeNumber(value.doubleValue());
            } else {
                jg.writeNumber(value.longValue());
            }
        }

        @Override
        public Number read(JsonParser jp, CoreSession session) throws IOException {
            if (jp.getCurrentToken() == JsonToken.VALUE_NUMBER_FLOAT) {
                return jp.getDoubleValue();
            } else {
                return jp.getLongValue();
            }
        }

        @Override
        public boolean isBuiltin() {
            return true;
        }

        public void register(ObjectCodecService service) {
            service.codecs.put(Integer.class, this);
            service.codecs.put(Integer.TYPE, this);
            service.codecs.put(Long.class, this);
            service.codecs.put(Long.TYPE, this);
            service.codecs.put(Double.class, this);
            service.codecs.put(Double.TYPE, this);
            service.codecs.put(Float.class, this);
            service.codecs.put(Float.TYPE, this);
            service.codecs.put(Short.class, this);
            service.codecs.put(Short.TYPE, this);
            service.codecs.put(Byte.class, this);
            service.codecs.put(Byte.TYPE, this);
            service.codecsByName.put(getType(), this);
        }
    }

    public static class DocumentAdapterCodec extends ObjectCodec<BusinessAdapter> {

        protected final DocumentAdapterDescriptor descriptor;

        @SuppressWarnings("unchecked")
        public DocumentAdapterCodec(DocumentAdapterDescriptor descriptor) {
            super(descriptor.getInterface());
            this.descriptor = descriptor;
        }

        @Override
        public String getType() {
            return descriptor.getInterface().getSimpleName();
        }

        public static void register(ObjectCodecService service, DocumentAdapterService adapterService) {
            for (DocumentAdapterDescriptor desc : adapterService.getAdapterDescriptors()) {
                if (!BusinessAdapter.class.isAssignableFrom(desc.getInterface())) {
                    continue;
                }
                DocumentAdapterCodec codec = new DocumentAdapterCodec(desc);
                if (service.codecsByName.containsKey(codec.getType())) {
                    log.warn("Be careful, you have already contributed an adapter with the same simple name:"
                            + codec.getType());
                    continue;
                }
                service.codecs.put(desc.getInterface(), codec);
                service.codecsByName.put(codec.getType(), codec);
            }
        }

        /**
         * When the object codec is called the stream is positioned on the first value. For inlined objects this is the
         * first value after the "entity-type" property. For non inlined objects this will be the object itself (i.e.
         * '{' or '[')
         *
         * @param jp
         * @return
         * @throws IOException
         */
        @Override
        public BusinessAdapter read(JsonParser jp, CoreSession session) throws IOException {
            if (jp.getCodec() == null) {
                jp.setCodec(new ObjectMapper());
            }
            BusinessAdapter fromBa = jp.readValueAs(type);
            DocumentModel doc = fromBa.getId() != null ? session.getDocument(new IdRef(fromBa.getId()))
                    : DocumentModelFactory.createDocumentModel(fromBa.getType());
            BusinessAdapter ba = doc.getAdapter(fromBa.getClass());

            // And finally copy the fields sets from the adapter
            for (String schema : fromBa.getDocument().getSchemas()) {
                DataModel dataModel = ba.getDocument().getDataModel(schema);
                DataModel fromDataModel = fromBa.getDocument().getDataModel(schema);

                for (String field : fromDataModel.getDirtyFields()) {
                    dataModel.setData(field, fromDataModel.getData(field));
                }
            }
            return ba;
        }
    }

}
