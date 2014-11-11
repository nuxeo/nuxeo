/*
 * (C) Copyright 2007-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Info about the fulltext configuration.
 */
public class ModelFulltext {

    public static final String PROP_TYPE_STRING = "string";

    public static final String PROP_TYPE_BLOB = "blob";

    /** All index names. */
    public Set<String> indexNames = new LinkedHashSet<String>();

    /** Indexes holding exactly one field. */
    public Map<String, String> fieldToIndexName = new HashMap<String, String>();

    /** Map of index to analyzer (may be null). */
    public Map<String, String> indexAnalyzer = new HashMap<String, String>();

    /** Map of index to catalog (may be null). */
    public Map<String, String> indexCatalog = new HashMap<String, String>();

    /** Indexes containing all simple properties. */
    public Set<String> indexesAllSimple = new HashSet<String>();

    /** Indexes containing all binaries properties. */
    public Set<String> indexesAllBinary = new HashSet<String>();

    /** Indexes for each specific simple property path. */
    public Map<String, Set<String>> indexesByPropPathSimple = new HashMap<String, Set<String>>();

    /** Indexes for each specific binary property path. */
    public Map<String, Set<String>> indexesByPropPathBinary = new HashMap<String, Set<String>>();

    /** Indexes for each specific simple property path excluded. */
    public Map<String, Set<String>> indexesByPropPathExcludedSimple = new HashMap<String, Set<String>>();

    /** Indexes for each specific binary property path excluded. */
    public Map<String, Set<String>> indexesByPropPathExcludedBinary = new HashMap<String, Set<String>>();

    // inverse of above maps
    public Map<String, Set<String>> propPathsByIndexSimple = new HashMap<String, Set<String>>();

    public Map<String, Set<String>> propPathsByIndexBinary = new HashMap<String, Set<String>>();

    public Map<String, Set<String>> propPathsExcludedByIndexSimple = new HashMap<String, Set<String>>();

    public Map<String, Set<String>> propPathsExcludedByIndexBinary = new HashMap<String, Set<String>>();

}
