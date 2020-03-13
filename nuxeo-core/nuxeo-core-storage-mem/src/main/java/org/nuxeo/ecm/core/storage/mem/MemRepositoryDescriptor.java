/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.mem;

import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryDescriptor;

/**
 * Memory Repository Descriptor.
 *
 * @since 8.1
 */
@XObject(value = "repository")
public class MemRepositoryDescriptor extends DBSRepositoryDescriptor {

    public MemRepositoryDescriptor() {
    }

    @Override
    public MemRepositoryDescriptor clone() {
        return (MemRepositoryDescriptor) super.clone();
    }

}
