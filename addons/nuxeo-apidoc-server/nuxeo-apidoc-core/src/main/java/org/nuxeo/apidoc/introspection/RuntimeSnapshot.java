/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.introspection;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.apidoc.api.SeamComponentInfo;
import org.nuxeo.apidoc.seam.SeamRuntimeIntrospector;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.runtime.api.Framework;

public class RuntimeSnapshot extends AbstractRuntimeSnapshot implements
        DistributionSnapshot {

    protected ServerInfo serverInfo;

    public RuntimeSnapshot() {
        buildServerInfo();
    }

    @Override
    public String getVersion() {
        return serverInfo.getVersion();
    }

    @Override
    public String getName() {
        return serverInfo.getName();
    }

    @Override
    protected Collection<BundleInfoImpl> getBundles() {
        return serverInfo.getBundles();
    }

    protected synchronized ServerInfo buildServerInfo() {
        if (serverInfo == null) {
            serverInfo = ServerInfo.build();
            spi.addAll(serverInfo.getAllSpi());
            initSnapshot();
        }
        return serverInfo;
    }

    public void initSeamComponents(HttpServletRequest request) {
        if (seamInitialized) {
            return;
        }
        seamComponents = SeamRuntimeIntrospector.listNuxeoComponents(request);
        for (SeamComponentInfo seamComp : seamComponents) {
            ((SeamComponentInfoImpl) seamComp).setVersion(getVersion());
        }
        seamInitialized = true;
    }

    @Override
    public void initOperations() {
        if (opsInitialized) {
            return;
        }
        OperationType[] ops;
        try {
            ops = Framework.getService(AutomationService.class).getOperations();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        for (OperationType op : ops) {
            operations.add(new OperationInfoImpl(op.getDocumentation(),
                    getVersion(), op.getType().getCanonicalName(),
                    op.getContributingComponent()));
        }
        opsInitialized = true;
    }

}
