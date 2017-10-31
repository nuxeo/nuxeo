/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.platform.audit.service.extension;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.audit.api.AuditStorage;

/**
 * @since 9.3
 */
@XObject("storage")
public class AuditStorageDescriptor {

    @XNode("@id")
    protected String id;

    @XNode("@class")
    protected Class<? extends AuditStorage> clazz;

    public String getId() {
        return id;
    }

    public Class<? extends AuditStorage> getClazz() {
        return clazz;
    }

    public AuditStorage newInstance() {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Cannot create audit storage of type " + clazz.getName(), e);
        }
    }
}
