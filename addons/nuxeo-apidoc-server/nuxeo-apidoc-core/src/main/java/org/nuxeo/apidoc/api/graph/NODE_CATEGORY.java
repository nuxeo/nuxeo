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

    RUNTIME("#000000"), CORE("#0000FF"), PLATFORM("#FF0000"), STUDIO("#008000");

    private NODE_CATEGORY(String color) {
        this.color = color;
    }

    private String color;

    public String toString() {
        return name();
    }

    public String getColor() {
        return color;
    }

    public static NODE_CATEGORY getCategory(BundleInfo bundle) {
        NODE_CATEGORY cat = introspect(bundle.getGroupId());
        if (cat == null) {
            cat = introspect(bundle.getArtifactId());
        }
        if (cat == null) {
            cat = introspect(bundle.getId());
        }
        if (cat == null) {
            cat = PLATFORM;
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

}
