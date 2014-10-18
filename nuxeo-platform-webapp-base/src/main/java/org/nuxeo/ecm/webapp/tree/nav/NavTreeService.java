/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.webapp.tree.nav;

import java.util.List;

import org.nuxeo.ecm.platform.actions.ActionService;
import org.nuxeo.ecm.webapp.directory.DirectoryTreeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Very simple component to manage Navigation tree registration.
 * <p>
 * Moved from module nuxeo-platform-virtual-navigation-web, originally added in
 * 5.6.
 *
 * @author Thierry Delprat
 * @author Thierry Martins
 * @since 6.0
 */
public class NavTreeService extends DefaultComponent {

    public static String NAVTREE_EP = "navigationTree";

    protected NavTreeRegistry registry;

    @Override
    public void activate(ComponentContext context) throws Exception {
        registry = new NavTreeRegistry();
    }

    @Override
    public void deactivate(ComponentContext context) {
        registry = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (NAVTREE_EP.equals(extensionPoint)) {
            NavTreeDescriptor contrib = (NavTreeDescriptor) contribution;
            registry.addContribution(contrib);
            getActionService().getActionRegistry().addAction(
                    contrib.getAction());
        }
    }

    /**
     * @since 5.6
     */
    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (NAVTREE_EP.equals(extensionPoint)) {
            NavTreeDescriptor contrib = (NavTreeDescriptor) contribution;
            registry.removeContribution(contrib);
            getActionService().getActionRegistry().removeAction(
                    contrib.getTreeId());
        }
    }

    public List<NavTreeDescriptor> getTreeDescriptors() {
        return registry.getTreeDescriptors(getDirectoryTreeService());
    }

    protected DirectoryTreeService getDirectoryTreeService() {
        return (DirectoryTreeService) Framework.getRuntime().getComponent(
                DirectoryTreeService.NAME);
    }

    /**
     * @since 6.0
     */
    protected ActionService getActionService() {
        return (ActionService) Framework.getRuntime().getComponent(
                ActionService.ID);
    }

    /**
     * Returns the last modified time of this service or of the
     * {@link DirectoryTreeService}, whichever the greater
     *
     * @since 5.6
     */
    @Override
    public Long getLastModified() {
        Long res = super.getLastModified();
        DirectoryTreeService treeService = getDirectoryTreeService();
        if (treeService != null) {
            Long other = treeService.getLastModified();
            if (res == null || (other != null && other.compareTo(res) > 0)) {
                res = other;
            }
        }
        return res;
    }
}
