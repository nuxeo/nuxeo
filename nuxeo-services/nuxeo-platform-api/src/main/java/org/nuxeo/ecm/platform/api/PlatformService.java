/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 * @deprecated use {@link Framework} API instead - Remove in 5.2.
 */
@SuppressWarnings({"ALL"})
@Deprecated
public class PlatformService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.api.PlatformService");

    private static final Log log = LogFactory.getLog(PlatformService.class);

    private static PlatformService instance;

    private RuntimeContext context;

    private Platform platform;


    public PlatformService() {
        instance = this;
    }

    public static PlatformService getInstance() {
        return instance;
    }

    public RuntimeContext getContext() {
        return context;
    }

    public Platform getPlatform() {
        return platform;
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        this.context = context.getRuntimeContext();
        String name = (String) context.getPropertyValue("platform-name");
        platform = new Platform(name);
        //ECM.setPlatform(platform);
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        platform.dispose();
        platform = null;
        this.context = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("servers")) {
            try {
                platform.addServer((ServerDescriptor) contribution);
            } catch (Exception e) {
                log.error("Failed to register platform module contribution: "
                        + ((ServerDescriptor) contribution).name, e);
            }
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("servers")) {
            platform.removeServer(((ServerDescriptor) contribution).name);
        }
    }

}
