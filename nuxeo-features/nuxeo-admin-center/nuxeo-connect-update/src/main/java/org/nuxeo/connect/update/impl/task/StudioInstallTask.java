/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.connect.update.impl.task;

import java.util.Map;

import org.nuxeo.connect.update.PackageException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.impl.RegistrationInfoImpl;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class StudioInstallTask extends InstallTask {

    @Override
    protected void doRun(Map<String, String> params) throws PackageException {
        super.doRun(params);
        // reload themes completely to avoid theme registry problems.
        reloadComponent("org.nuxeo.theme.services.ThemeService");
    }

    public void reloadComponent(String name) throws PackageException {
        try {
            RegistrationInfoImpl ri = (RegistrationInfoImpl)Framework.getRuntime().getComponentManager().getRegistrationInfo(new ComponentName(name));
            if (ri != null) {
                ri.reload();
            }
        } catch(Exception e) {
            throw new PackageException("Failed to reload component: "+name, e);
        }
    }
}
