/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.api.graph;

import org.nuxeo.apidoc.api.BundleInfo;

/**
 * @since 11.1
 */
public enum NODE_CATEGORY {

    RUNTIME, CORE, PLATFORM, STUDIO;

    public String toString() {
        return name();
    }

    public static NODE_CATEGORY guessCategory(BundleInfo bundle) {
        NODE_CATEGORY cat = guessCategory(bundle.getGroupId(), null);
        if (cat == null) {
            cat = guessCategory(bundle.getArtifactId(), null);
        }
        if (cat == null) {
            cat = guessCategory(bundle.getId(), PLATFORM);
        }
        return cat;
    }

    public static NODE_CATEGORY guess(String id) {
        return guessCategory(id, PLATFORM);
    }

    protected static NODE_CATEGORY guessCategory(String id, NODE_CATEGORY defaultValue) {
        NODE_CATEGORY cat = introspect(id);
        if (cat == null) {
            cat = defaultValue;
        }
        return cat;
    }

    protected static NODE_CATEGORY introspect(String source) {
        for (NODE_CATEGORY item : NODE_CATEGORY.values()) {
            if (contains(source, item.name())) {
                return item;
            }
        }
        return null;
    }

    protected static boolean contains(String source, String content) {
        if (source == null) {
            return false;
        }
        return source.toLowerCase().contains(content.toLowerCase());
    }

    public static NODE_CATEGORY getCategory(String cat, NODE_CATEGORY defaultValue) {
        for (NODE_CATEGORY ecat : NODE_CATEGORY.values()) {
            if (ecat.name().equalsIgnoreCase(cat)) {
                return ecat;
            }
        }
        return defaultValue;
    }

}
