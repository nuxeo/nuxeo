/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.pgjson;

import java.io.Serializable;

/**
 * A database column and its type, together with the DBS key it corresponds to.
 *
 * @since 11.1
 */
public class PGColumn {

    public final String key;

    public final String name;

    public final PGType type;

    public PGColumn(String key, String name, PGType type) {
        this.key = key;
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + key + ',' + name + ',' + type + ')';
    }

    /** A column and an associated value. */
    public static class PGColumnAndValue {

        public final PGColumn column;

        public final Serializable value;

        public PGColumnAndValue(PGColumn column, Serializable value) {
            this.column = column;
            this.value = value;
        }
    }

}
