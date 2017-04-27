/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.directory.mongodb;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;

/**
 * Helper for serialization/deserialization of BSON objects
 *
 * @since 9.1
 */
public class MongoDBSerializationHelper {

    public static final String MONGODB_ID = "_id";

    public static final String MONGODB_SEQ = "seq";

    private MongoDBSerializationHelper() {
        // empty
    }

    /**
     * Create a BSON object with a single field from a pair key/value
     *
     * @param key the key which corresponds to the field id in the object
     * @param value the value which corresponds to the field value in the object
     * @return the new BSON object
     */
    public static Document fieldMapToBson(String key, Object value) {
        return fieldMapToBson(Collections.singletonMap(key, value));
    }

    /**
     * Create a BSON object from a map
     *
     * @param fieldMap a map of keys/values
     * @return the new BSON object
     */
    public static Document fieldMapToBson(Map<String, Object> fieldMap) {
        Document doc = new Document();
        for (Map.Entry<String, Object> entry : fieldMap.entrySet()) {
            Object val = valueToBson(entry.getValue());
            if (val != null) {
                doc.put(entry.getKey(), val);
            }
        }
        return doc;
    }

    /**
     * Cast an object according to its instance
     *
     * @param value the object to transform
     * @return the BSON object
     */
    public static Object valueToBson(Object value) {
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            return fieldMapToBson(map);
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> values = (List<Object>) value;
            return listToBson(values);
        } else if (value instanceof Object[]) {
            return listToBson(Arrays.asList((Object[]) value));
        } else {
            return serializableToBson(value);
        }
    }

    protected static List<Object> listToBson(List<Object> values) {
        List<Object> objects = new ArrayList<>(values.size());
        for (Object value : values) {
            objects.add(valueToBson(value));
        }
        return objects;
    }

    protected static Object serializableToBson(Object value) {
        if (value instanceof Calendar) {
            return ((Calendar) value).getTime();
        }
        return value;
    }

    /**
     * Create a map from a BSON object
     *
     * @param doc the BSON object to parse
     * @return the new map
     */
    public static Map<String, Object> bsonToFieldMap(Document doc) {
        Map<String, Object> fieldMap = new HashMap<>();
        for (String key : doc.keySet()) {
            if (MONGODB_ID.equals(key)) {
                // skip native id
                continue;
            }
            fieldMap.put(key, bsonToValue(doc.get(key)));
        }
        return fieldMap;
    }

    protected static Serializable bsonToValue(Object value) {
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            if (list.isEmpty()) {
                return null;
            } else {
                Class<?> klass = Object.class;
                for (Object o : list) {
                    if (o != null) {
                        klass = scalarToSerializableClass(o.getClass());
                        break;
                    }
                }
                if (Document.class.isAssignableFrom(klass)) {
                    List<Serializable> l = new ArrayList<>(list.size());
                    for (Object o : list) {
                        l.add((Serializable) bsonToFieldMap((Document) o));
                    }
                    return (Serializable) l;
                } else {
                    // turn the list into a properly-typed array
                    Object[] array = (Object[]) Array.newInstance(klass, list.size());
                    int i = 0;
                    for (Object o : list) {
                        array[i++] = scalarToSerializable(o);
                    }
                    return array;
                }
            }
        } else if (value instanceof Document) {
            return (Serializable) bsonToFieldMap((Document) value);
        } else {
            return scalarToSerializable(value);
        }
    }

    protected static Class<?> scalarToSerializableClass(Class<?> klass) {
        if (Date.class.isAssignableFrom(klass)) {
            return Calendar.class;
        }
        return klass;
    }

    protected static Serializable scalarToSerializable(Object value) {
        if (value instanceof Date) {
            Calendar cal = Calendar.getInstance();
            cal.setTime((Date) value);
            return cal;
        }
        return (Serializable) value;
    }

}
