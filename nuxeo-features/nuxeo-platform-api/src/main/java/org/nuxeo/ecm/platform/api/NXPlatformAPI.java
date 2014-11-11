/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: NXPlatformAPI.java 21079 2007-06-21 20:12:19Z bstefanescu $
 */

package org.nuxeo.ecm.platform.api;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.ServiceManagement;

/**
 * Facade for services provided by NXPlatformAPI module.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 * @deprecated use new service API {@link ServiceManagement}
 */
// XXX: never used. Remove?
@Deprecated
public final class NXPlatformAPI {

    private NXPlatformAPI() {
    }

    /**
     * @return the platform service.
     */
    public static PlatformService getPlatformService() {
        return (PlatformService) Framework.getRuntime().getComponent(
                PlatformService.NAME);
    }

}
