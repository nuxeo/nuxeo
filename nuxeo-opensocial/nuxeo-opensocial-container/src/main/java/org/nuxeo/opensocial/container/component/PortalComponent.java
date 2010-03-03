/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.container.component;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class PortalComponent extends DefaultComponent {

    private static final String PORTAL_COMPONENT_NAME = "org.nuxeo.opensocial.container.config";

    private static final String XP_CONFIG = "portalConfig";

    private PortalConfig config;

    public static PortalComponent getInstance() {
        return (PortalComponent) Framework.getRuntime().getComponent(
                PORTAL_COMPONENT_NAME);
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (XP_CONFIG.equals(extensionPoint)) {
            PortalConfig contrib = (PortalConfig) contribution;
            this.config = contrib;
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        super.unregisterContribution(contribution, extensionPoint, contributor);
    }

    public PortalConfig getConfig() {
        return config;
    }

}
