/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id: FieldAdapterManager.java 28460 2008-01-03 15:34:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.el;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The FieldAdapterManager fills the gap between the storage and the display structures.
 * <p>
 * The Display representation of a DataModel is a set of JSF Beans There are mainly 3 cases:
 * <p>
 * 1 - Perfect match: the JSF components generate a bean that can be directly stored ie: String ...
 * <p>
 * 2 - Type Mismatch: The JSF component generate a bean that is not of the right type ie: The JSF generate a Date
 * whereas the Core expect a Calendar type.
 * <p>
 * 3 - Structure Mismatch: The JSF bean must be split in several fields ie: The uploaded file is one object, but the
 * core expect at least 2 separate fields (filename and content)
 *
 * @author Thierry Delprat
 */
public final class FieldAdapterManager {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(FieldAdapterManager.class);

    // Utility class.
    private FieldAdapterManager() {
    }

    /**
     * Sets value adapting it for storage.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Object getValueForStorage(Object value) {
        if (value instanceof Date) {
            value = getDateAsCalendar((Date) value);
        } else if (value instanceof BigDecimal) {
            value = getBigDecimalAsLong((BigDecimal) value);
        } else if (value instanceof Object[]) {
            Object[] array = (Object[]) value;
            Class<?> oldType = array.getClass().getComponentType();
            Class<?> newType = getComponentTypeForStorage(oldType);
            Object[] newArray = (Object[]) Array.newInstance(newType, array.length);
            for (int i = 0; i < array.length; i++) {
                newArray[i] = getValueForStorage(array[i]);
            }
            value = newArray;
        } else if (value instanceof List) {
            List list = (List) value;
            for (int i = 0; i < list.size(); i++) {
                list.set(i, getValueForStorage(list.get(i)));
            }
        } else if (value instanceof Map) {
            Map<Object, Object> map = (Map) value;
            Map<Object, Object> newMap = new HashMap<Object, Object>();
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                newMap.put(entry.getKey(), getValueForStorage(entry.getValue()));
            }
            value = newMap;
        }
        // TODO: maybe handle list diffs (?)
        return value;
    }

    /**
     * Returns component type that will be used to store objects of given component type.
     */
    public static Class<?> getComponentTypeForStorage(Class<?> componentType) {
        Class<?> newType = componentType;
        if (componentType.equals(Date.class)) {
            newType = Calendar.class;
        }
        return newType;
    }

    /**
     * Gets value adapting it for display.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Object getValueForDisplay(Object value) {
        if (value instanceof Calendar) {
            value = getCalendarAsDate((Calendar) value);
        } else if (value instanceof Object[]) {
            Object[] array = (Object[]) value;
            Class<?> oldType = array.getClass().getComponentType();
            Class<?> newType = getComponentTypeForDisplay(oldType);
            Object[] newArray = (Object[]) Array.newInstance(newType, array.length);
            for (int i = 0; i < array.length; i++) {
                newArray[i] = getValueForDisplay(array[i]);
            }
            value = newArray;
        } else if (value instanceof List) {
            List list = (List) value;
            for (int i = 0; i < list.size(); i++) {
                list.set(i, getValueForDisplay(list.get(i)));
            }
        } else if (value instanceof Map) {
            Map<Object, Object> map = (Map) value;
            Map<Object, Object> newMap = new HashMap<Object, Object>();
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                newMap.put(entry.getKey(), getValueForDisplay(entry.getValue()));
            }
            value = newMap;
        }
        return value;
    }

    /**
     * Returns component type that will be used to display objects of given component type.
     */
    public static Class<?> getComponentTypeForDisplay(Class<?> componentType) {
        Class<?> newType = componentType;
        if (componentType.equals(Calendar.class)) {
            newType = Date.class;
        }
        return newType;
    }

    // Fake converters for now
    // XXX make an extension point to register Adapters
    // XXX update TypeManager to handle Adapter configuration

    private static Calendar getDateAsCalendar(Date value) {
        Calendar calValue = Calendar.getInstance();
        calValue.setTime(value);
        return calValue;
    }

    private static Date getCalendarAsDate(Calendar value) {
        return value.getTime();
    }

    /**
     * @since 7.1
     */
    private static Long getBigDecimalAsLong(BigDecimal value) {
        return value.longValueExact();
    }

}
