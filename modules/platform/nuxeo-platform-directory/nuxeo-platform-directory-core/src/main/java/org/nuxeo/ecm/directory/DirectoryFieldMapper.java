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
        fieldMapping = new HashMap<>();
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
        Set<String> mappedFields = new HashSet<>();
        for (String fieldName : fieldNames) {
            mappedFields.add(getBackendField(fieldName));
        }
        return mappedFields;
    }

}
