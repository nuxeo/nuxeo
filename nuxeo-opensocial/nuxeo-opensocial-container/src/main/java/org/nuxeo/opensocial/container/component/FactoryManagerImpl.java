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

import org.nuxeo.opensocial.container.component.api.FactoryManager;
import org.nuxeo.opensocial.container.factory.api.ContainerManager;
import org.nuxeo.opensocial.container.factory.api.GadgetManager;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class FactoryManagerImpl extends DefaultComponent implements
        FactoryManager {

    private static final String XP_CONFIG = "factoryConfig";

    private GadgetManager gadgetManager;

    private ContainerManager containerManager;

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (XP_CONFIG.equals(extensionPoint)) {
            FactoryConfig contrib = (FactoryConfig) contribution;
            gadgetManager = (GadgetManager) Class.forName(
                    contrib.getGadgetFactory()).newInstance();
            containerManager = (ContainerManager) Class.forName(
                    contrib.getContainerFactory()).newInstance();
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        super.unregisterContribution(contribution, extensionPoint, contributor);
    }

    public GadgetManager getGadgetFactory() {
        return gadgetManager;
    };

    public ContainerManager getContainerFactory() {
        return containerManager;
    };

}
