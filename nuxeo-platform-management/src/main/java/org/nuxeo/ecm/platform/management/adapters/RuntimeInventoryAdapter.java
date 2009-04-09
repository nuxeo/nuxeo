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
 *     matic
 */
package org.nuxeo.ecm.platform.management.adapters;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RegistrationInfo;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 *
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

    public Set<String> getAvailableComponents() {
        Set<String> names = new HashSet<String>();
        for (RegistrationInfo info : availableComponents()) {
            names.add(info.getName().getRawName());
        }
        return names;
    }

    public int getAvailableComponentsCount() {
        return runtimeService.getComponentManager().getRegistrations().size();
    }

    public int getPendingComponentsCount() {
        return pendingComponentsName().size();
    }

    public Set<String> getPendingComponentsName() {
        Set<String> names = new HashSet<String>();
        for (ComponentName componentName : pendingComponentsName()) {
            names.add(componentName.getRawName());
        }
        return names;
    }

    public String getDescription() {
        return runtimeService.getDescription();
    }

    public String getHome() {
        try {
            return runtimeService.getHome().getCanonicalPath();
        } catch (IOException e) {
            throw new ClientRuntimeException("cannot get path", e);
        }
    }

    public String getName() {
        return runtimeService.getName();
    }

    public String getVersion() {
        return runtimeService.getVersion().toString();
    }

    protected boolean isTreeBound = false;

    public boolean isTreeBound() {
        return isTreeBound;
    }

    public void bindTree() {
        if (isTreeBound) {
            throw new IllegalArgumentException("tree already bound");
        }
        isTreeBound = true;
        factory.bindTree();
    }

    public void unbindTree() {
        if (!isTreeBound) {
            throw new IllegalArgumentException("tree not bound");
        }
        isTreeBound = false;
        factory.unbindTree();
    }

}
