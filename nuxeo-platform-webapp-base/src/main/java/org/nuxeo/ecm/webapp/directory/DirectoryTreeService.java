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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private static final Log log = LogFactory.getLog(DirectoryTreeService.class);

    protected Map<String, DirectoryTreeDescriptor> registry;

    public DirectoryTreeDescriptor getDirectoryTreeDescriptor(String treeName) {

        DirectoryTreeDescriptor desc = registry.get(treeName);
        if (desc.getEnabled()) {
            return desc;
        }
        else {
            return null;
        }
    }

    @Override
    public void activate(ComponentContext context) {
        registry = new HashMap<String, DirectoryTreeDescriptor>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        registry = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {

        DirectoryTreeDescriptor descriptor = (DirectoryTreeDescriptor) contribution;

        if (registry.containsKey(descriptor.getName())) {
            DirectoryTreeDescriptor existing_descriptor =registry.get(descriptor.getName());
            existing_descriptor.merge(descriptor);
            log.debug("merged DirectoryTreeDescriptor: " + descriptor.getName());
        }
        else {
            registry.put(descriptor.getName(), descriptor);
            log.debug("registered DirectoryTreeDescriptor: " + descriptor.getName());
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {

        DirectoryTreeDescriptor descriptor = (DirectoryTreeDescriptor) contribution;
        registry.remove(descriptor.getName());
        log.debug("unregistered DirectoryTreeDescriptor: "
                + descriptor.getName());
    }

    public List<String> getDirectoryTrees() {
        List<String> directoryTrees = new ArrayList<String>();
        for (DirectoryTreeDescriptor desc : registry.values()) {
            if (desc.getEnabled()) {
                directoryTrees.add(desc.getName());
            }
        }
        Collections.sort(directoryTrees);
        return directoryTrees;
    }

    /**
     * Returns only the enabled Directory Trees marked
     * as being also Navigation Trees.
     *
     * @since 5.4
     */
    public List<String> getNavigationDirectoryTrees() {
        List<String> directoryTrees = new ArrayList<String>();
        for (DirectoryTreeDescriptor desc : registry.values()) {
            if (desc.getEnabled() && desc.isNavigationTree()) {
                directoryTrees.add(desc.getName());
            }
        }
        Collections.sort(directoryTrees);
        return directoryTrees;
    }

}
