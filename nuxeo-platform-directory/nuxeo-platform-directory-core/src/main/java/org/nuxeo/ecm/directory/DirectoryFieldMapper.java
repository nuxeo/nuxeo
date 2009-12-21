/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.directory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DirectoryFieldMapper {

    private final Map<String, String> fieldMapping;

    public DirectoryFieldMapper(Map<String, String> fieldMapping) {
        this.fieldMapping = fieldMapping;
    }

    public DirectoryFieldMapper() {
        fieldMapping = new HashMap<String, String>();
    }

    // Direct Mapping
    public String getBackendField(String fieldName) {
        String backendField = fieldMapping.get(fieldName);
        if (backendField != null) {
            return backendField;
        } else {
            return fieldName;
        }
    }

    // Reverse Mapping
    public String getDirectoryField(String backEndFieldName) {
        if (fieldMapping.containsValue(backEndFieldName)) {
            for (String key : fieldMapping.keySet()) {
                if (fieldMapping.get(key).equals(backEndFieldName)) {
                    return key;
                }
            }
        }
        return backEndFieldName;
    }

    // Direct Mapping for a set
    public Set<String> getBackendFields(Set<String> fieldNames) {
        Set<String> mappedFields = new HashSet<String>();
        for (String fieldName : fieldNames) {
            mappedFields.add(getBackendField(fieldName));
        }
        return mappedFields;
    }

}
