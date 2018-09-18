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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.api.repository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Info about the fulltext configuration.
 *
 * @since 10.3 in this package
 */
public class FulltextConfiguration {

    public static final String ROOT_TYPE = "Root";

    /** All index names. */
    public final Set<String> indexNames = new LinkedHashSet<>();

    /** Indexes holding exactly one field. */
    public final Map<String, String> fieldToIndexName = new HashMap<>();

    /** Indexes containing all simple properties. */
    public final Set<String> indexesAllSimple = new HashSet<>();

    /** Indexes containing all binaries properties. */
    public final Set<String> indexesAllBinary = new HashSet<>();

    /** Indexes for each specific simple property path. */
    public final Map<String, Set<String>> indexesByPropPathSimple = new HashMap<>();

    /** Indexes for each specific binary property path. */
    // DBSTransactionState.findDirtyDocuments expects this to contain unprefixed versions for schemas
    // without prefix, like "content/data".
    public final Map<String, Set<String>> indexesByPropPathBinary = new HashMap<>();

    /** Indexes for each specific simple property path excluded. */
    public final Map<String, Set<String>> indexesByPropPathExcludedSimple = new HashMap<>();

    /** Indexes for each specific binary property path excluded. */
    public final Map<String, Set<String>> indexesByPropPathExcludedBinary = new HashMap<>();

    // inverse of above maps
    public final Map<String, Set<String>> propPathsByIndexSimple = new HashMap<>();

    public final Map<String, Set<String>> propPathsByIndexBinary = new HashMap<>();

    public final Map<String, Set<String>> propPathsExcludedByIndexSimple = new HashMap<>();

    public final Map<String, Set<String>> propPathsExcludedByIndexBinary = new HashMap<>();

    public final Set<String> excludedTypes = new HashSet<>();

    public final Set<String> includedTypes = new HashSet<>();

    public boolean fulltextSearchDisabled;

    public int fulltextFieldSizeLimit;

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
