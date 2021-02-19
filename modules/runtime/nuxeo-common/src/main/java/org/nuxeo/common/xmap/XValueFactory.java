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
import java.time.Duration;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.utils.DurationUtils;
import org.w3c.dom.Node;

/**
 * Value factories are used to decode values from XML strings.
 * <p>
 * To register a new factory for a given XMap instance use the method
 * {@link XMap#setValueFactory(Class, XValueFactory)}.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class XValueFactory<T> {

    static final Map<Class<?>, XValueFactory<?>> defaultFactories = new Hashtable<>();

    public abstract T deserialize(Context context, String value);

    public abstract String serialize(Context context, T value);

    public final T getElementValue(Context context, Node element, boolean trim) {
        String text = element.getTextContent();
        return deserialize(context, trim ? text.trim() : text);
    }

    public final T getAttributeValue(Context context, Node element, String name) {
        Node at = element.getAttributes().getNamedItem(name);
        return at != null ? deserialize(context, at.getNodeValue()) : null;
    }

    public static <C> void addFactory(Class<C> klass, XValueFactory<C> factory) {
        defaultFactories.put(klass, factory);
    }

    public static <C> XValueFactory<C> getFactory(Class<C> type) {
        @SuppressWarnings("unchecked")
        XValueFactory<C> factory = (XValueFactory<C>) defaultFactories.get(type);
        return factory;
    }

    public static <C> C getValue(Context context, Class<C> klass, String value) {
        @SuppressWarnings("unchecked")
        XValueFactory<C> factory = (XValueFactory<C>) defaultFactories.get(klass);
        if (factory == null) {
            return null;
        }
        return factory.deserialize(context, value);
    }

    public static final XValueFactory<String> STRING = new XValueFactory<>() {
        @Override
        public String deserialize(Context context, String value) {
            return value;
        }

        @Override
        public String serialize(Context context, String value) {
            return value;
        }
    };

    public static final XValueFactory<Integer> INTEGER = new XValueFactory<>() {
        @Override
        public Integer deserialize(Context context, String value) {
            return Integer.valueOf(value);
        }

        @Override
        public String serialize(Context context, Integer value) {
            return value.toString();
        }
    };

    public static final XValueFactory<Long> LONG = new XValueFactory<>() {
        @Override
        public Long deserialize(Context context, String value) {
            return Long.valueOf(value);
        }

        @Override
        public String serialize(Context context, Long value) {
            return value.toString();
        }
    };

    public static final XValueFactory<Double> DOUBLE = new XValueFactory<>() {
        @Override
        public Double deserialize(Context context, String value) {
            return Double.valueOf(value);
        }

        @Override
        public String serialize(Context context, Double value) {
            return value.toString();
        }
    };

    public static final XValueFactory<Float> FLOAT = new XValueFactory<>() {
        @Override
        public Float deserialize(Context context, String value) {
            return Float.valueOf(value);
        }

        @Override
        public String serialize(Context context, Float value) {
            return value.toString();
        }
    };

    public static final XValueFactory<Boolean> BOOLEAN = new XValueFactory<>() {
        @Override
        public Boolean deserialize(Context context, String value) {
            return Boolean.valueOf(value);
        }

        @Override
        public String serialize(Context context, Boolean value) {
            return value.toString();
        }
    };

    public static final XValueFactory<Date> DATE = new XValueFactory<>() {
        private final DateFormat df = DateFormat.getDateInstance();

        @Override
        public Date deserialize(Context context, String value) {
            try {
                return df.parse(value);
            } catch (ParseException e) {
                return null;
            }
        }

        @Override
        public String serialize(Context context, Date value) {
            return df.format(value);
        }
    };

    public static final XValueFactory<File> FILE = new XValueFactory<>() {
        @Override
        public File deserialize(Context context, String value) {
            return new File(value);
        }

        @Override
        public String serialize(Context context, File value) {
            return value.getName();
        }
    };

    public static final XValueFactory<URL> URL = new XValueFactory<>() {
        @Override
        public URL deserialize(Context context, String value) {
            try {
                return new URL(value);
            } catch (MalformedURLException e) {
                return null;
            }
        }

        @Override
        public String serialize(Context context, URL value) {
            return value.toString();
        }
    };

    @SuppressWarnings("rawtypes")
    public static final XValueFactory<Class> CLASS = new XValueFactory<>() {
        @Override
        public Class deserialize(Context context, String value) {
            if (StringUtils.isBlank(value)) {
                return null;
            }
            try {
                return context.loadClass(value);
            } catch (ClassNotFoundException e) {
                throw new XMapException("Cannot load class: " + value, e);
            }
        }

        @Override
        public String serialize(Context context, Class value) {
            return value.getName();
        }
    };

    public static final XValueFactory<Resource> RESOURCE = new XValueFactory<>() {
        @Override
        public Resource deserialize(Context context, String value) {
            return new Resource(context, value);
        }

        @Override
        public String serialize(Context context, Resource value) {
            return value.toString();
        }
    };

    public static final XValueFactory<Duration> DURATION = new XValueFactory<>() {

        @Override
        public Duration deserialize(Context context, String value) {
            return DurationUtils.parse(value);
        }

        @Override
        public String serialize(Context context, Duration value) {
            // always use JDK format
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

        addFactory(Duration.class, DURATION);
    }

}
