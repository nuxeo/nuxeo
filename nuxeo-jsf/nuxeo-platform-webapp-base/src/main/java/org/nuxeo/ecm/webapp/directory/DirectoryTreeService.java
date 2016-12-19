/*
 * (C) Copyright 2007-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.webapp.directory;

import java.util.List;

import org.nuxeo.ecm.platform.actions.ActionService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Register directory tree configurations to make them available to the DirectoryTreeManagerBean to build
 * DirectoryTreeNode instances.
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
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        DirectoryTreeDescriptor descriptor = (DirectoryTreeDescriptor) contribution;
        registry.addContribution(descriptor);
        getActionService().addAction(descriptor.getAction());
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        DirectoryTreeDescriptor descriptor = (DirectoryTreeDescriptor) contribution;
        registry.removeContribution(descriptor);
        getActionService().removeAction(descriptor.getAction().getId());
    }

    /**
     * @since 6.0
     */
    protected ActionService getActionService() {
        return (ActionService) Framework.getRuntime().getComponent(ActionService.ID);
    }

    public List<String> getDirectoryTrees() {
        return registry.getDirectoryTrees();
    }

    /**
     * Returns only the enabled Directory Trees marked as being also Navigation Trees.
     *
     * @since 5.4
     */
    public List<String> getNavigationDirectoryTrees() {
        return registry.getNavigationDirectoryTrees();
    }

}
