/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ArrayProperty;
import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.StringProperty;

/**
 * Finds the strings in a document (string properties).
 * <p>
 * This class is not thread-safe.
 *
 * @since 10.3
 */
public class StringsExtractor {

    protected DocumentModel document;

    // paths for which we extract fulltext, or null for all
    protected Set<String> includedPaths;

    protected Set<String> excludedPaths;

    // collected strings
    protected List<String> strings;

    /**
     * Finds strings from the document for a given set of included and excluded paths.
     * <p>
     * Paths must be specified with a schema prefix in all cases (normalized).
     *
     * @param document the document
     * @param includedPaths the paths to include, or {@code null} for all paths
     * @param excludedPaths the paths to exclude, or {@code null} for none
     * @return a list of strings (each string is never {@code null})
     */
    public List<String> findStrings(DocumentModel document, Set<String> includedPaths, Set<String> excludedPaths) {
        this.document = document;
        this.includedPaths = includedPaths;
        this.excludedPaths = excludedPaths;
        strings = new ArrayList<>();
        for (String schema : document.getSchemas()) {
            for (Property property : document.getPropertyObjects(schema)) {
                String path = property.getField().getName().getPrefixedName();
                if (!path.contains(":")) {
                    // add schema name as prefix if the schema doesn't have a prefix
                    path = property.getSchema().getName() + ":" + path;
                }
                findStrings(property, path);
            }
        }
        return strings;
    }

    protected boolean isInterestingPath(String path) {
        if (excludedPaths != null && excludedPaths.contains(path)) {
            return false;
        }
        return includedPaths == null || includedPaths.contains(path);
    }

    protected void findStrings(Property property, String path) {
        if (property instanceof StringProperty) {
            if (isInterestingPath(path)) {
                Serializable value = property.getValue();
                if (value instanceof String) {
                    strings.add((String) value);
                }
            }
        } else if (property instanceof ArrayProperty) {
            if (isInterestingPath(path)) {
                Serializable value = property.getValue();
                if (value instanceof Object[]) {
                    for (Object v : (Object[]) value) {
                        if (v instanceof String) {
                            strings.add((String) v);
                        }
                    }
                }
            }
        } else if (property instanceof ComplexProperty) {
            for (Property p : ((ComplexProperty) property).getChildren()) {
                String pp = p.getField().getName().getPrefixedName();
                findStrings(p, path + '/' + pp);
            }
        } else if (property instanceof ListProperty) {
            for (Property p : (ListProperty) property) {
                findStrings(p, path + "/*");
            }
        }
    }
}
