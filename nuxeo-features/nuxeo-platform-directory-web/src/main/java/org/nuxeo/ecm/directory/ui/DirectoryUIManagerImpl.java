/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id:  $
 */

package org.nuxeo.ecm.directory.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
 */
public class DirectoryUIManagerImpl extends DefaultComponent implements DirectoryUIManager {

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
    public void activate(ComponentContext context) {
        super.activate(context);
        registry = new LinkedHashMap<>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        registry = null;
        super.deactivate(context);
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
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
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (!extensionPoint.equals(DIRECTORIES_EP_NAME)) {
            log.warn("Unknown extension point: " + extensionPoint);
            return;
        }

        DirectoryUIDescriptor desc = (DirectoryUIDescriptor) contribution;
        registry.remove(desc.getName());
    }

    @Override
    public DirectoryUI getDirectoryInfo(String directoryName) {
        return registry.get(directoryName);
    }

    @Override
    public List<String> getDirectoryNames() {
        List<String> dirNames = new ArrayList<>(registry.keySet());
        Collections.sort(dirNames, String.CASE_INSENSITIVE_ORDER);
        return dirNames;
    }

}
