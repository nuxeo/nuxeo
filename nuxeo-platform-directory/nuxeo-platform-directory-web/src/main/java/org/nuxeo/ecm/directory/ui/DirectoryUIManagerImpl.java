/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id:  $
 */

package org.nuxeo.ecm.directory.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.directory.api.ui.DirectoryUI;
import org.nuxeo.ecm.directory.api.ui.DirectoryUIDescriptor;
import org.nuxeo.ecm.directory.api.ui.DirectoryUIManager;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Component managing directory ui information
 *
 * @author Anahide Tchertchian
 *
 */
public class DirectoryUIManagerImpl extends DefaultComponent implements
        DirectoryUIManager {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DirectoryUIManagerImpl.class);

    protected static final String DIRECTORIES_EP_NAME = "directories";

    protected Map<String, DirectoryUI> registry;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(DirectoryUIManager.class)) {
            return (T) this;
        }
        return null;
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        registry = new LinkedHashMap<String, DirectoryUI>();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        registry = null;
        super.deactivate(context);
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (!extensionPoint.equals(DIRECTORIES_EP_NAME)) {
            log.warn("Unknown extension point: " + extensionPoint);
            return;
        }

        DirectoryUIDescriptor desc = (DirectoryUIDescriptor) contribution;
        String name = desc.getName();
        boolean disabled = Boolean.FALSE.equals(desc.isEnabled());
        if (registry.containsKey(name)) {
            log.info("Overriding " + name);
        }
        if (disabled) {
            registry.remove(name);
        } else {
            registry.put(name, desc);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (!extensionPoint.equals(DIRECTORIES_EP_NAME)) {
            log.warn("Unknown extension point: " + extensionPoint);
            return;
        }

        DirectoryUIDescriptor desc = (DirectoryUIDescriptor) contribution;
        registry.remove(desc.getName());
    }

    public DirectoryUI getDirectoryInfo(String directoryName)
            throws ClientException {
        return registry.get(directoryName);
    }

    public List<String> getDirectoryNames() throws ClientException {
        return new ArrayList<String>(registry.keySet());
    }

}
