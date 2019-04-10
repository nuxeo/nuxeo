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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.importer.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import org.nuxeo.common.utils.Path;

/**
 *
 * Collects meta-data from a FileSystem and manage inheritence
 *
 * @author Thierry Delprat
 *
 */
public class MetadataCollector {

    public static final boolean staticInherit = true;

    public static final boolean useIntrospection = false;

    public static String DATE_FORMAT = "MM/dd/yyyy";

    public static String LIST_SEPARATOR = "|";

    public static String REGEXP_LIST_SEPARATOR = "\\|";

    public static String ARRAY_SEPARATOR = "||";

    public static String REGEXP_ARRAY_SEPARATOR = "\\|\\|";

    protected Map<String, Map<String, Serializable>> collectedMetadata = new HashMap<String, Map<String, Serializable>>();

    protected ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    protected String normalizePath(String contextPath) {
        if (contextPath != null) {
            return contextPath = new Path(contextPath).removeTrailingSeparator().toString();
        }
        return null;
    }

    public void addPropertiesFromStrings(String contextPath,
            Map<String, String> properties) {
        Map<String, Serializable> collectedProperties = new HashMap<String, Serializable>();
        for (String name : properties.keySet()) {
            Serializable value = parseFromString(name, properties.get(name));
            collectedProperties.put(name, value);
        }
        addProperties(contextPath, collectedProperties);
    }

    public void addProperties(String contextPath,
            Map<String, Serializable> collectedProperties) {
        try {
            lock.writeLock().lock();
            contextPath = normalizePath(contextPath);
            if (staticInherit) {
                Path path = new Path(contextPath);
                while (!path.isEmpty() && !path.isRoot()) {
                    path = path.removeLastSegments(1);
                    Map<String, Serializable> parentProperties = collectedMetadata.get(path.toString());
                    if (parentProperties != null) {
                        for (String name : parentProperties.keySet()) {
                            if (!collectedProperties.containsKey(name)) {
                                collectedProperties.put(name,
                                        parentProperties.get(name));
                            }
                        }
                    }
                }
            }
            collectedMetadata.put(contextPath, collectedProperties);
        } finally {
            lock.writeLock().unlock();
        }

    }

    protected static Pattern numPattern = Pattern.compile("([0-9\\,\\.\\+\\-]+)");

    protected Serializable parseFromString(String name, String value) {

        Serializable prop = value;
        if (useIntrospection) {
            throw new UnsupportedOperationException(
                    "Introspection mode not available");
        } else {
            if (value.contains(ARRAY_SEPARATOR)) {
                prop = value.split(REGEXP_ARRAY_SEPARATOR);
            } else if (value.contains(LIST_SEPARATOR)) {
                List<Serializable> lstprop = new ArrayList<Serializable>();
                String[] parts = value.split(REGEXP_LIST_SEPARATOR);
                for (String part : parts) {
                    lstprop.add(parseFromString(name, part));
                }
                prop = (Serializable) lstprop;
            } else if (value.contains("/")) {
                try {
                    Date date = new SimpleDateFormat(DATE_FORMAT).parse(value);
                    Calendar cal = new GregorianCalendar();
                    cal.setTime(date);
                    prop = cal;
                } catch (ParseException e) {
                }
            }
        }
        return prop;
    }

    public Serializable getProperty(String contextPath, String name) {

        Map<String, Serializable> props = getProperties(contextPath);
        if (props != null) {
            try {
                lock.readLock().lock();
                return props.get(name);
            } finally {
                lock.readLock().unlock();
            }
        } else {
            return null;
        }
    }

    public Map<String, Serializable> getProperties(String contextPath) {

        contextPath = normalizePath(contextPath);

        try {
            lock.readLock().lock();
            Map<String, Serializable> props = collectedMetadata.get(contextPath);

            if (props == null) {
                Path path = new Path(contextPath);
                while (props == null && !path.isEmpty() && !path.isRoot()) {
                    path = path.removeLastSegments(1);
                    props = collectedMetadata.get(path.toString());
                }
            }

            if (props != null) {
                props = Collections.unmodifiableMap(props);
            }
            return props;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addPropertyFile(File propertyFile) throws Exception {

        Properties mdProperties = new Properties();
        mdProperties.load(new FileInputStream(propertyFile));

        Map<String, String> stringMap = new HashMap<String, String>();
        Enumeration names = mdProperties.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            stringMap.put(name, mdProperties.getProperty(name));
        }
        String contextPath = new Path(propertyFile.getAbsolutePath()).removeLastSegments(
                1).toString();
        addPropertiesFromStrings(contextPath, stringMap);
    }

}
