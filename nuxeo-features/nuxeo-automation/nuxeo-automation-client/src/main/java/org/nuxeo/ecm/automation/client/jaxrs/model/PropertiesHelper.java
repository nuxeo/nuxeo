/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.automation.client.jaxrs.model;

import java.util.Date;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PropertiesHelper {

    public static String getString(Object v, String defValue) {
        if (v == null) {
            return defValue;
        }
        if (v.getClass() == String.class) {
            return v.toString();
        }
        throw new IllegalArgumentException("Property is not a scalar");
    }

    public static Boolean getBoolean(Object v, Boolean defValue) {
        if (v == null) {
            return defValue;
        }
        if (v.getClass() == String.class) {
            return Boolean.valueOf(v.toString());
        }
        throw new IllegalArgumentException("Property is not a scalar");
    }

    public static Long getLong(Object v, Long defValue) {
        if (v == null) {
            return defValue;
        }
        if (v.getClass() == String.class) {
            return Long.valueOf(v.toString());
        }
        throw new IllegalArgumentException("Property is not a scalar");
    }

    public static Double getDouble(Object v, Double defValue) {
        if (v == null) {
            return defValue;
        }
        if (v.getClass() == String.class) {
            return Double.valueOf(v.toString());
        }
        throw new IllegalArgumentException("Property is not a scalar");
    }

    public static Date getDate(Object v, Date defValue) {
        if (v == null) {
            return defValue;
        }
        if (v.getClass() == String.class) {
            return DateUtils.parseDate(v.toString());
        }
        throw new IllegalArgumentException("Property is not a scalar");
    }

    public static PropertyList getList(Object v, PropertyList defValue) {
        if (v == null) {
            return defValue;
        }
        if (v instanceof PropertyList) {
            return (PropertyList)v;
        }
        throw new IllegalArgumentException("Property is not a list");
    }

    public static PropertyMap getMap(Object v, PropertyMap defValue) {
        if (v == null) {
            return defValue;
        }
        if (v instanceof PropertyMap) {
            return (PropertyMap)v;
        }
        throw new IllegalArgumentException("Property is not a list");
    }

}
