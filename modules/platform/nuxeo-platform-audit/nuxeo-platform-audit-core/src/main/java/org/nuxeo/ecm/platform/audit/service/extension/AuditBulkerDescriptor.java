/*
 * (C) Copyright 2010, 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.audit.service.extension;

import java.lang.reflect.Constructor;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.AuditBulker;
import org.nuxeo.ecm.platform.audit.service.DefaultAuditBulker;

/**
 * @deprecated since 10.10, audit bulker is now handled with nuxeo-stream, no replacement
 */
@Deprecated
@XObject("bulk")
public class AuditBulkerDescriptor {

    @XNode("@class")
    protected Class<? extends AuditBulker> klass = DefaultAuditBulker.class;

    @XNode("timeout")
    public int timeout = 1000; // 1 second

    @XNode("size")
    public int size = 1000;

    public AuditBulker newInstance(AuditBackend backend) {
        try {
            Constructor<? extends AuditBulker> declaredConstructor = klass.getDeclaredConstructor(AuditBackend.class, AuditBulkerDescriptor.class);
            declaredConstructor.setAccessible(true);
            return declaredConstructor.newInstance(backend, this);
        } catch (ReflectiveOperationException cause) {
            throw new RuntimeException("Cannot create audit backend of type " + klass.getName(), cause);
        }
    }
}
