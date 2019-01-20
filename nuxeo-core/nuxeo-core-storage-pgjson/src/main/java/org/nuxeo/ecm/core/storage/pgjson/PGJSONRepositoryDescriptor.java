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

import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryDescriptor;

/**
 * PostgreSQL+JSON Repository Descriptor.
 *
 * @since 11.1
 */
@XObject(value = "repository")
public class PGJSONRepositoryDescriptor extends DBSRepositoryDescriptor {

    public PGJSONRepositoryDescriptor() {
    }

    @Override
    public PGJSONRepositoryDescriptor clone() {
        return (PGJSONRepositoryDescriptor) super.clone();
    }

    public void merge(PGJSONRepositoryDescriptor other) {
        super.merge(other);
    }

}
