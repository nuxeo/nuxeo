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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.importer.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Collects meta-data from a FileSystem and manage inheritence
 *
 * @author Thierry Delprat
 */
public class MetadataCollector {

    public static final boolean staticInherit = true;

    public static final boolean useIntrospection = false;

    public static String DATE_FORMAT = "MM/dd/yyyy";

    public static String LIST_SEPARATOR = "|";

    public static String REGEXP_LIST_SEPARATOR = "\\|";

    public static String ARRAY_SEPARATOR = "||";

    public static String REGEXP_ARRAY_SEPARATOR = "\\|\\|";

    protected Map<String, Map<String, Serializable>> collectedMetadata = new HashMap<>();

    protected ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public void addPropertiesFromStrings(String contextPath, Map<String, String> properties) {
        Map<String, Serializable> collectedProperties = new HashMap<>();
        for (String name : properties.keySet()) {
            Serializable value = parseFromString(name, properties.get(name));
            collectedProperties.put(name, value);
        }
        addProperties(contextPath, collectedProperties);
    }

    public void addProperties(String contextPath, Map<String, Serializable> collectedProperties) {
        try {
            lock.writeLock().lock();
            contextPath = FilenameUtils.normalizeNoEndSeparator(contextPath);
            if (staticInherit) {
                File file = new File(contextPath);
                while (!StringUtils.isEmpty(file.getParent())) {
                    file = file.getParentFile();
                    Map<String, Serializable> parentProperties = collectedMetadata.get(file.toString());
                    if (parentProperties != null) {
                        for (String name : parentProperties.keySet()) {
                            if (!collectedProperties.containsKey(name)) {
                                collectedProperties.put(name, parentProperties.get(name));
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
            throw new UnsupportedOperationException("Introspection mode not available");
        } else {
            if (value.contains(ARRAY_SEPARATOR)) {
                prop = value.split(REGEXP_ARRAY_SEPARATOR);
            } else if (value.contains(LIST_SEPARATOR)) {
                List<Serializable> lstprop = new ArrayList<>();
                String[] parts = value.split(REGEXP_LIST_SEPARATOR);
                for (String part : parts) {
                    lstprop.add(parseFromString(name, part));
                }
                prop = (Serializable) lstprop;
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

        contextPath = FilenameUtils.normalizeNoEndSeparator(contextPath);

        try {
            lock.readLock().lock();
            Map<String, Serializable> props = collectedMetadata.get(contextPath);

            if (props == null) {
                File file = new File(contextPath);
                while (props == null && !StringUtils.isEmpty(file.getParent())) {
                    file = file.getParentFile();
                    props = collectedMetadata.get(file.getPath());
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

    public void addPropertyFile(File propertyFile) throws IOException {

        Properties mdProperties = new Properties();
        mdProperties.load(new FileInputStream(propertyFile));

        Map<String, String> stringMap = new HashMap<>();
        Enumeration names = mdProperties.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            stringMap.put(name, mdProperties.getProperty(name));
        }
        String contextPath = new File(propertyFile.getPath()).getParent();
        addPropertiesFromStrings(contextPath, stringMap);
    }

}
