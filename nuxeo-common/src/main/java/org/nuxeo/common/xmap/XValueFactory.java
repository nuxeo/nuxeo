/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.common.xmap;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

import org.w3c.dom.Node;

/**
 * Value factories are used to decode values from XML strings.
 * <p>
 * To register a new factory for a given XMap instance use the method
 * {@link XMap#setValueFactory(Class, XValueFactory)}.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class XValueFactory {

    static final Map<Class<?>, XValueFactory> defaultFactories = new Hashtable<>();

    public abstract Object deserialize(Context context, String value);

    public abstract String serialize(Context context, Object value);

    public final Object getElementValue(Context context, Node element, boolean trim) {
        String text = element.getTextContent();
        return deserialize(context, trim ? text.trim() : text);
    }

    public final Object getAttributeValue(Context context, Node element, String name) {
        Node at = element.getAttributes().getNamedItem(name);
        return at != null ? deserialize(context, at.getNodeValue()) : null;
    }

    public static void addFactory(Class klass, XValueFactory factory) {
        defaultFactories.put(klass, factory);
    }

    public static XValueFactory getFactory(Class type) {
        return defaultFactories.get(type);
    }

    public static Object getValue(Context context, Class klass, String value) {
        XValueFactory factory = defaultFactories.get(klass);
        if (factory == null) {
            return null;
        }
        return factory.deserialize(context, value);
    }

    public static final XValueFactory STRING = new XValueFactory() {
        @Override
        public Object deserialize(Context context, String value) {
            return value;
        }

        @Override
        public String serialize(Context context, Object value) {
            return value.toString();
        }
    };

    public static final XValueFactory INTEGER = new XValueFactory() {
        @Override
        public Object deserialize(Context context, String value) {
            return Integer.valueOf(value);
        }

        @Override
        public String serialize(Context context, Object value) {
            return value.toString();
        }
    };

    public static final XValueFactory LONG = new XValueFactory() {
        @Override
        public Object deserialize(Context context, String value) {
            return Long.valueOf(value);
        }

        @Override
        public String serialize(Context context, Object value) {
            return value.toString();
        }
    };

    public static final XValueFactory DOUBLE = new XValueFactory() {
        @Override
        public Object deserialize(Context context, String value) {
            return Double.valueOf(value);
        }

        @Override
        public String serialize(Context context, Object value) {
            return value.toString();
        }
    };

    public static final XValueFactory FLOAT = new XValueFactory() {
        @Override
        public Object deserialize(Context context, String value) {
            return Float.valueOf(value);
        }

        @Override
        public String serialize(Context context, Object value) {
            return value.toString();
        }
    };

    public static final XValueFactory BOOLEAN = new XValueFactory() {
        @Override
        public Object deserialize(Context context, String value) {
            return Boolean.valueOf(value);
        }

        @Override
        public String serialize(Context context, Object value) {
            return value.toString();
        }
    };

    public static final XValueFactory DATE = new XValueFactory() {
        private final DateFormat df = DateFormat.getDateInstance();

        @Override
        public Object deserialize(Context context, String value) {
            try {
                return df.parse(value);
            } catch (ParseException e) {
                return null;
            }
        }

        @Override
        public String serialize(Context context, Object value) {
            Date date = (Date) value;
            return df.format(date);
        }
    };

    public static final XValueFactory FILE = new XValueFactory() {
        @Override
        public Object deserialize(Context context, String value) {
            return new File(value);
        }

        @Override
        public String serialize(Context context, Object value) {
            File file = (File) value;
            return file.getName();
        }
    };

    public static final XValueFactory URL = new XValueFactory() {
        @Override
        public Object deserialize(Context context, String value) {
            try {
                return new URL(value);
            } catch (MalformedURLException e) {
                return null;
            }
        }

        @Override
        public String serialize(Context context, Object value) {
            return value.toString();
        }
    };

    public static final XValueFactory CLASS = new XValueFactory() {
        @Override
        public Object deserialize(Context context, String value) {
            try {
                return context.loadClass(value);
            } catch (ClassNotFoundException e) {
                throw new XMapException("Cannot load class: " + value, e);
            }
        }

        @Override
        public String serialize(Context context, Object value) {
            Class<?> clazz = (Class<?>) value;
            return clazz.getName();
        }
    };

    public static final XValueFactory RESOURCE = new XValueFactory() {
        @Override
        public Object deserialize(Context context, String value) {
            return new Resource(context.getResource(value));
        }

        @Override
        public String serialize(Context context, Object value) {
            return value.toString();
        }
    };

    static {
        addFactory(String.class, STRING);
        addFactory(Integer.class, INTEGER);
        addFactory(Long.class, LONG);
        addFactory(Double.class, DOUBLE);
        addFactory(Date.class, DATE);
        addFactory(Boolean.class, BOOLEAN);
        addFactory(File.class, FILE);
        addFactory(URL.class, URL);

        addFactory(int.class, INTEGER);
        addFactory(long.class, LONG);
        addFactory(double.class, DOUBLE);
        addFactory(float.class, FLOAT);
        addFactory(boolean.class, BOOLEAN);

        addFactory(Class.class, CLASS);
        addFactory(Resource.class, RESOURCE);
    }

}
