/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
        mdProperties.load(new FileInputStream(propertyFile));
        Map<String, String> stringMap = new HashMap<String, String>();
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
