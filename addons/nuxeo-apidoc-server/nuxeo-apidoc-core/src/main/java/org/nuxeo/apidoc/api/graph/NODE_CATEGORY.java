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

    RUNTIME(0), CORE(1), PLATFORM(2), STUDIO(3);

    private NODE_CATEGORY(int index) {
        this.index = index;
    }

    private int index;

    public String toString() {
        return name();
    }

    public int getIndex() {
        return index;
    }

    public String getColor() {
        return "TODO";
    }

    public static NODE_CATEGORY getCategory(BundleInfo bundle) {
        // TODO: introspect bundle
        return RUNTIME;
    }

}
