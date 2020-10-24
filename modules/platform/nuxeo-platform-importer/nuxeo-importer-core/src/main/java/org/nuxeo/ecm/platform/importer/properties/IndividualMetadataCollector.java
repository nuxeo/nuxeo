/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *    Mariana Cedica
 */
package org.nuxeo.ecm.platform.importer.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * The properties are mapped by the collector using as key the path of the file/folder to import.
 */
public class IndividualMetadataCollector extends MetadataCollector {

    @Override
    public void addPropertyFile(File propertyFile) throws IOException {
        String contextPath = propertyFile.getAbsolutePath();
        addPropertyFile(propertyFile, contextPath);
    }

    public void addPropertyFile(File propertyFile, String contextPath) throws IOException {
        Properties mdProperties = new Properties();
        try (var in = new FileInputStream(propertyFile)) {
            mdProperties.load(in);
        }
        Map<String, String> stringMap = new HashMap<>();
        Enumeration<?> names = mdProperties.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            stringMap.put(name, mdProperties.getProperty(name));
        }
        addPropertiesFromStrings(contextPath, stringMap);
    }

    public Map<String, Serializable> getProperties(File file) {
        return getProperties(file.getPath());
    }
}
