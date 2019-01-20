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

/**
 * The database-level column types, including per-type parameters like length.
 *
 * @since 11.1
 */
public class PGColumn {

    protected final String key;

    protected final String name;

    protected final PGType type;

    public PGColumn(String key, String name, PGType type) {
        this.key = key;
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + key + ',' + name + ',' + type + ')';
    }

}
