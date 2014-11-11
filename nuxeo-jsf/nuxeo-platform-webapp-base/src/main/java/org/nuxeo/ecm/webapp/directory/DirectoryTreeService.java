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
 * $Id: DirectoryTreeService.java 20619 2007-06-16 21:49:26Z sfermigier $
 */
package org.nuxeo.ecm.webapp.directory;

import java.util.List;

import org.nuxeo.ecm.platform.actions.ActionService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Register directory tree configurations to make them available to the
 * DirectoryTreeManagerBean to build DirectoryTreeNode instances.
 *
 * @author <a href="mailto:ogrisel@nuxeo.com">Olivier Grisel</a>
 */
public class DirectoryTreeService extends DefaultComponent {

    public static final String NAME = DirectoryTreeService.class.getName();

    protected DirectoryTreeRegistry registry;

    public DirectoryTreeDescriptor getDirectoryTreeDescriptor(String treeName) {
        return registry.getDirectoryTreeDescriptor(treeName);
    }

    @Override
    public void activate(ComponentContext context) {
        registry = new DirectoryTreeRegistry();
    }

    @Override
    public void deactivate(ComponentContext context) {
        registry = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        DirectoryTreeDescriptor descriptor = (DirectoryTreeDescriptor) contribution;
        registry.addContribution(descriptor);
        getActionService().getActionRegistry().addAction(descriptor.getAction());
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        DirectoryTreeDescriptor descriptor = (DirectoryTreeDescriptor) contribution;
        registry.removeContribution(descriptor);
        getActionService().getActionRegistry().removeAction(
                descriptor.getAction().getId());
    }

    /**
     * @since 6.0
     */
    protected ActionService getActionService() {
        return (ActionService) Framework.getRuntime().getComponent(
                ActionService.ID);
    }

    public List<String> getDirectoryTrees() {
        return registry.getDirectoryTrees();
    }

    /**
     * Returns only the enabled Directory Trees marked as being also Navigation
     * Trees.
     *
     * @since 5.4
     */
    public List<String> getNavigationDirectoryTrees() {
        return registry.getNavigationDirectoryTrees();
    }

}
