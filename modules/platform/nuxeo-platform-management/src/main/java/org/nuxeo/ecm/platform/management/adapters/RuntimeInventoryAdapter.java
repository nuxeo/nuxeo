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
 *     matic
 */
package org.nuxeo.ecm.platform.management.adapters;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RegistrationInfo;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class RuntimeInventoryAdapter implements RuntimeInventoryMBean {

    protected final RuntimeService runtimeService = Framework.getRuntime();

    protected final RuntimeInventoryFactory factory;

    public RuntimeInventoryAdapter(RuntimeInventoryFactory factory) {
        this.factory = factory;
    }

    protected Collection<RegistrationInfo> availableComponents() {
        return runtimeService.getComponentManager().getRegistrations();
    }

    protected Collection<ComponentName> pendingComponentsName() {
        return runtimeService.getComponentManager().getActivatingRegistrations();
    }

    @Override
    public Set<String> getAvailableComponents() {
        Set<String> names = new HashSet<>();
        for (RegistrationInfo info : availableComponents()) {
            names.add(info.getName().getRawName());
        }
        return names;
    }

    @Override
    public int getAvailableComponentsCount() {
        return runtimeService.getComponentManager().getRegistrations().size();
    }

    @Override
    public int getPendingComponentsCount() {
        return pendingComponentsName().size();
    }

    @Override
    public Set<String> getPendingComponentsName() {
        Set<String> names = new HashSet<>();
        for (ComponentName componentName : pendingComponentsName()) {
            names.add(componentName.getRawName());
        }
        return names;
    }

    @Override
    public String getDescription() {
        return runtimeService.getDescription();
    }

    @Override
    public String getHome() {
        try {
            return runtimeService.getHome().getCanonicalPath();
        } catch (IOException e) {
            throw new NuxeoException("cannot get path", e);
        }
    }

    @Override
    public String getName() {
        return runtimeService.getName();
    }

    @Override
    public String getVersion() {
        return runtimeService.getVersion().toString();
    }

    protected boolean isTreeBound = false;

    @Override
    public boolean isTreeBound() {
        return isTreeBound;
    }

    @Override
    public void bindTree() {
        if (isTreeBound) {
            throw new IllegalArgumentException("tree already bound");
        }
        isTreeBound = true;
        factory.bindTree();
    }

    @Override
    public void unbindTree() {
        if (!isTreeBound) {
            throw new IllegalArgumentException("tree not bound");
        }
        isTreeBound = false;
        factory.unbindTree();
    }

}
