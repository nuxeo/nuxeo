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
package org.nuxeo.ecm.automation.client.model;

import java.util.Date;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PropertiesHelper {

    private PropertiesHelper() {
    }

    public static boolean isBlob(Object v) {
        return v instanceof Blob;
    }

    public static boolean isMap(Object v) {
        return v instanceof PropertyMap;
    }

    public static boolean isList(Object v) {
        return v instanceof PropertyList;
    }

    public static boolean isScalar(Object v) {
        return !isBlob(v) && !isList(v) && !isMap(v);
    }

    public static String getString(Object v, String defValue) {
        if (v == null) {
            return defValue;
        }
        if (v.getClass() == String.class) {
            return v.toString();
        }
        throw new IllegalArgumentException("Property is not a scalar: " + v);
    }

    public static Boolean getBoolean(Object v, Boolean defValue) {
        if (v == null) {
            return defValue;
        }
        if (v.getClass() == String.class) {
            return Boolean.valueOf(v.toString());
        }
        throw new IllegalArgumentException("Property is not a scalar: " + v);
    }

    public static Long getLong(Object v, Long defValue) {
        if (v == null) {
            return defValue;
        }
        if (v.getClass() == String.class) {
            return Long.valueOf(v.toString());
        }
        throw new IllegalArgumentException("Property is not a scalar: " + v);
    }

    public static Double getDouble(Object v, Double defValue) {
        if (v == null) {
            return defValue;
        }
        if (v.getClass() == String.class) {
            return Double.valueOf(v.toString());
        }
        throw new IllegalArgumentException("Property is not a scalar: " + v);
    }

    public static Date getDate(Object v, Date defValue) {
        if (v == null) {
            return defValue;
        }
        if (v.getClass() == String.class) {
            return DateUtils.parseDate(v.toString());
        } else {
            return (Date) v;
        }
        // throw new IllegalArgumentException("Property is not a scalar: " + v);
    }

    public static PropertyList getList(Object v, PropertyList defValue) {
        if (v == null) {
            return defValue;
        }
        if (v instanceof PropertyList) {
            return (PropertyList) v;
        }
        throw new IllegalArgumentException("Property is not a list: " + v);
    }

    public static PropertyMap getMap(Object v, PropertyMap defValue) {
        if (v == null) {
            return defValue;
        }
        if (v instanceof PropertyMap) {
            return (PropertyMap) v;
        }
        throw new IllegalArgumentException("Property is not a map: " + v);
    }

}
