/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.jaxrs.io;

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
import java.util.LinkedHashMap;
import java.util.Map;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.core.schema.utils.DateParser;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ObjectCodecService {

    protected Map<Class<?>, ObjectCodec<?>> codecs;
    protected Map<String, ObjectCodec<?>> codecsByName;

    protected Map<Class<?>, ObjectCodec<?>> _codecs;
    protected Map<String, ObjectCodec<?>> _codecsByName;


    public ObjectCodecService() {
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
        write (baos, object, preetyPrint);
        return baos.toString("UTF-8");
    }

    public void write(OutputStream out, Object object) throws IOException {
        write (out, object, false);
    }

    public void write(OutputStream out, Object object, boolean prettyPint) throws IOException {
        JsonGenerator jg = JsonWriter.createGenerator(out);
        if (prettyPint) {
            jg.useDefaultPrettyPrinter();
        }
        write(jg, object);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void write(JsonGenerator jg, Object object) throws IOException {
        if (object == null) {
            jg.writeStringField("entity-type", "null");
            jg.writeFieldName("value");
            jg.writeNull();
        } else {
            Class<?> clazz = object.getClass();
            ObjectCodec<?> codec = getCodec(clazz);
            if (codec == null) {
                writeGenericObject(jg, clazz, object);
            } else {
                jg.writeStartObject();
                jg.writeStringField("entity-type", codec.getType());
                jg.writeFieldName("value");
                ((ObjectCodec)codec).write(jg, object);
                jg.writeEndObject();
            }
        }
        jg.flush();
    }

    public Object read(String json) throws IOException, ClassNotFoundException {
        return read(json, null);
    }

    public Object read(String json, ClassLoader cl) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(json.getBytes());
        return read(in, cl);
    }

    public Object read(InputStream in) throws IOException, ClassNotFoundException {
        return read(in, null);
    }

    public Object read(InputStream in, ClassLoader cl) throws IOException, ClassNotFoundException {
        return read(JsonWriter.getFactory().createJsonParser(in), cl);
    }

    public Object read(JsonParser jp, ClassLoader cl) throws IOException, ClassNotFoundException {
        JsonToken tok = jp.getCurrentToken();
        if (tok == null) {
            tok = jp.nextToken();
        }
        if (tok == JsonToken.START_OBJECT) {
            tok = jp.nextToken();
        } else if (tok != JsonToken.FIELD_NAME) {
            throw new IllegalStateException("Invalid parser state. Current token must be either start_object or field_name");
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
        ObjectCodec<?> codec = codecs.get(name);
        if (codec == null) {
            return readGenericObject(jp, name, cl);
        } else {
            return codec.read(jp);
        }
    }

    public Object readNode(JsonNode node, ClassLoader cl) throws IOException, ClassNotFoundException {
        // Handle simple scalar types
        if (node.isNumber()) {
            return node.getNumberValue();
        } else if (node.isBoolean()) {
            return node.getBooleanValue();
        } else if (node.isTextual()) {
            return node.getTextValue();
        }
        JsonNode entityTypeNode = node.get("entity-type");
        JsonNode valueNode = node.get("value");
        if (entityTypeNode != null && entityTypeNode.isTextual()) {
            // handle structured entity with an explicit type declaration
            if (valueNode == null) {
                return null;
            }
            String type = entityTypeNode.getTextValue();
            ObjectCodec<?> codec = codecsByName.get(type);
            JsonParser valueParser = valueNode.traverse();
            if (valueParser.getCurrentToken() == null) {
                valueParser.nextToken();
            }
            if (codec == null) {
                return readGenericObject(valueParser, type, cl);
            } else {
                return codec.read(valueParser);
            }
        }
        // fallback to returning the original json node
        return node;
    }

    public Object readNode(JsonNode node) throws IOException, ClassNotFoundException {
        return readNode(node, null);
    }

    protected final void writeGenericObject(JsonGenerator jg, Class<?> clazz, Object object) throws IOException {
        jg.writeStartObject();
        if (clazz.isPrimitive()) {
            if (clazz == Boolean.TYPE) {
                jg.writeStringField("entity-type", "boolean");
                jg.writeBooleanField("value", (Boolean)object);
            } else if (clazz == Double.TYPE || clazz == Float.TYPE) {
                jg.writeStringField("entity-type", "number");
                jg.writeNumberField("value", ((Number)object).doubleValue());
            } else if (clazz == Integer.TYPE || clazz == Long.TYPE || clazz == Short.TYPE || clazz == Byte.TYPE) {
                jg.writeStringField("entity-type", "number");
                jg.writeNumberField("value", ((Number)object).longValue());
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

    protected final Object readGenericObject(JsonParser jp, String name, ClassLoader cl) throws IOException, ClassNotFoundException {
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
        Class<?> clazz = cl.loadClass(name);
        return jp.readValueAs(clazz);
    }


    public static class StringCodec extends ObjectCodec<String> {
        public StringCodec() {
            super (String.class);
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
        public String read(JsonParser jp) throws IOException {
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
            super (Date.class);
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
        public Date read(JsonParser jp) throws IOException {
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
            super (Calendar.class);
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
        public Calendar read(JsonParser jp) throws IOException {
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
            super (Boolean.class);
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
        public Boolean read(JsonParser jp) throws IOException {
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
            super (Number.class);
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
        public Number read(JsonParser jp) throws IOException {
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

    public static void main(String[] args) throws Exception {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        ArrayList<Object> list = new ArrayList<Object>();
        list.add("v1");
        list.add(2);
        list.add(new Date());
        map.put("list", list);
        map.put("k", "v");
        map.put("k2", "v");
        map.put("k1", "v");
        ObjectCodecService s = new ObjectCodecService();
        String json = s.toString(map, true);
        System.out.println(json);
        System.out.println("================");
        System.out.println(s.toString(s.read(json), true));
    }

}
