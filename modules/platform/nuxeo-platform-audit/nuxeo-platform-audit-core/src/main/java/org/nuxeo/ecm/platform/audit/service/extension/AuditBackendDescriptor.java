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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.platform.audit.service.extension;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.DefaultAuditBackend;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Descriptor to configure / contribute a Backend for Audit service
 *
 * @author tiry
 */
@XObject("backend")
public class AuditBackendDescriptor {

    @XNode("@class")
    protected Class<? extends AuditBackend> klass = DefaultAuditBackend.class;

    @XNode("require")
    String requiredComponent;

    public int getApplicationStartedOrder() {
        if (StringUtils.isEmpty(requiredComponent)) {
            return 1000;
        }
        return ((DefaultComponent)Framework.getRuntime().getComponent(requiredComponent)).getApplicationStartedOrder()+1;
    }

    public Class<? extends AuditBackend> getKlass() {
        return klass;
    }

    public AuditBackend newInstance(NXAuditEventsService component) {
        try {
            return klass.getDeclaredConstructor(NXAuditEventsService.class, AuditBackendDescriptor.class).newInstance(component, this);
        } catch (ReflectiveOperationException cause) {
            throw new RuntimeException("Cannot create audit backend of type " + klass.getName(), cause);
        }
    }

}
