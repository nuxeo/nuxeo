/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.storage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Info about the fulltext configuration.
 */
public class FulltextConfiguration {

    public static final String ROOT_TYPE = "Root";

    public static final String PROP_TYPE_STRING = "string";

    public static final String PROP_TYPE_BLOB = "blob";

    /** All index names. */
    public final Set<String> indexNames = new LinkedHashSet<String>();

    /** Indexes holding exactly one field. */
    public final Map<String, String> fieldToIndexName = new HashMap<String, String>();

    /** Map of index to analyzer (may be null). */
    public final Map<String, String> indexAnalyzer = new HashMap<String, String>();

    /** Map of index to catalog (may be null). */
    public final Map<String, String> indexCatalog = new HashMap<String, String>();

    /** Indexes containing all simple properties. */
    public final Set<String> indexesAllSimple = new HashSet<String>();

    /** Indexes containing all binaries properties. */
    public final Set<String> indexesAllBinary = new HashSet<String>();

    /** Indexes for each specific simple property path. */
    public final Map<String, Set<String>> indexesByPropPathSimple = new HashMap<String, Set<String>>();

    /** Indexes for each specific binary property path. */
    public final Map<String, Set<String>> indexesByPropPathBinary = new HashMap<String, Set<String>>();

    /** Indexes for each specific simple property path excluded. */
    public final Map<String, Set<String>> indexesByPropPathExcludedSimple = new HashMap<String, Set<String>>();

    /** Indexes for each specific binary property path excluded. */
    public final Map<String, Set<String>> indexesByPropPathExcludedBinary = new HashMap<String, Set<String>>();

    // inverse of above maps
    public final Map<String, Set<String>> propPathsByIndexSimple = new HashMap<String, Set<String>>();

    public final Map<String, Set<String>> propPathsByIndexBinary = new HashMap<String, Set<String>>();

    public final Map<String, Set<String>> propPathsExcludedByIndexSimple = new HashMap<String, Set<String>>();

    public final Map<String, Set<String>> propPathsExcludedByIndexBinary = new HashMap<String, Set<String>>();

    public final Set<String> excludedTypes = new HashSet<String>();

    public final Set<String> includedTypes = new HashSet<String>();

    public FulltextConfiguration() {
    }

    public boolean isFulltextIndexable(String typeName) {
        if (ROOT_TYPE.equals(typeName)) {
            return false;
        }
        if (includedTypes.contains(typeName) || (includedTypes.isEmpty() && !excludedTypes.contains(typeName))) {
            return true;
        }
        return false;
    }

}
