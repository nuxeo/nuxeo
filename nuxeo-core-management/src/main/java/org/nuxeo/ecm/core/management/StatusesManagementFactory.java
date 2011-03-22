/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

        AdministrativeStatusManagerImpl adminStatus = Framework.getLocalService(AdministrativeStatusManagerImpl.class);
        service.registerResource(
                "adminStatus",
                ObjectNameFactory.formatQualifiedName(CoreManagementComponent.NAME)
                        + ",status=administrative",
                AdministrativeStatusManagerImpl.class, adminStatus);

        ProbeManager runner = Framework.getLocalService(ProbeManager.class);
        service.registerResource(
                "probeStatus",
                ObjectNameFactory.formatQualifiedName(CoreManagementComponent.NAME)
                        + ",status=probes", ProbeManagerImpl.class, runner);
        for (ProbeInfo info : runner.getAllProbeInfos()) {
            doQualifyNames((ProbeInfoImpl) info);
            service.registerResource(info.getShortcutName(),
                    info.getQualifiedName(), ProbeInfoImpl.class, info);
        }
    }

}
