/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.management;

import org.nuxeo.ecm.core.management.api.ProbeInfo;
import org.nuxeo.ecm.core.management.api.ProbeManager;
import org.nuxeo.ecm.core.management.probes.ProbeInfoImpl;
import org.nuxeo.ecm.core.management.probes.ProbeManagerImpl;
import org.nuxeo.ecm.core.management.statuses.AdministrativeStatusManagerImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.AbstractResourceFactory;
import org.nuxeo.runtime.management.ObjectNameFactory;

public class StatusesManagementFactory extends AbstractResourceFactory {

    protected void doQualifyNames(ProbeInfoImpl info) {
        info.setShortcutName(info.getDescriptor().getShortcut());
        info.setQualifiedName(info.getDescriptor().getQualifiedName());
        if (info.getQualifiedName() == null) {
            info.setQualifiedName(ObjectNameFactory.formatQualifiedName(CoreManagementComponent.NAME)
                    + ",status=probes,probe=" + info.getShortcutName());
        }
    }

    @Override
    public void registerResources() {

        AdministrativeStatusManagerImpl adminStatus = Framework.getService(AdministrativeStatusManagerImpl.class);
        service.registerResource("adminStatus", ObjectNameFactory.formatQualifiedName(CoreManagementComponent.NAME)
                + ",status=administrative", AdministrativeStatusManagerImpl.class, adminStatus);

        ProbeManager runner = Framework.getService(ProbeManager.class);
        service.registerResource("probeStatus", ObjectNameFactory.formatQualifiedName(CoreManagementComponent.NAME)
                + ",status=probes", ProbeManagerImpl.class, runner);
        for (ProbeInfo info : runner.getAllProbeInfos()) {
            doQualifyNames((ProbeInfoImpl) info);
            service.registerResource(info.getShortcutName(), info.getQualifiedName(), ProbeInfoImpl.class, info);
        }
    }

}
