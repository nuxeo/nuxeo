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
package org.nuxeo.webengine.management.adapters;

import javax.management.ObjectName;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ObjectNameFactory;
import org.nuxeo.runtime.management.ResourceFactory;
import org.nuxeo.runtime.management.ResourceFactoryDescriptor;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.ExtensionPoint;
import org.nuxeo.runtime.model.RegistrationInfo;

/**
 * @author matic
 * 
 */
public class RuntimeInventoryMBeanAdapterFactory implements ResourceFactory {

    public RuntimeInventoryMBeanAdapterFactory(
            ResourceFactoryDescriptor descriptor) {
        inventoryName = ObjectNameFactory.getObjectName("name=runtime,type=factory");
    }

    protected final ObjectName inventoryName;

    public void registerResources(Callback callback) {
        callback.invokeFor(inventoryName, RuntimeInventoryMBean.class,
                new RuntimeInventoryMBeanAdapter());
        for (RegistrationInfo info : Framework.getRuntime().getComponentManager().getRegistrations()) {
            doInventoryComponent(callback, info);
        }
    }

    protected static ObjectName doGetObjectName(String formattedName) {
        return ObjectNameFactory.getObjectName(formattedName
                + ",info=inventory");
    }

    protected void doInventoryComponent(Callback callback, RegistrationInfo info) {
        String formattedName = ObjectNameFactory.formatName(info.getName());
        callback.invokeFor(doGetObjectName(formattedName),
                ComponentInventoryMBean.class,
                new ComponentInventoryMBeanAdapter(info));
        for (ExtensionPoint extensionPoint : info.getExtensionPoints()) {
            doInventoryExtensionPoint(callback, formattedName, extensionPoint);
        }
        for (Extension extension : info.getExtensions()) {
            doInventoryExtension(callback, formattedName, extension);
        }
    }

    protected void doInventoryExtensionPoint(Callback callback,
            String formattedName, ExtensionPoint extensionPoint) {
        callback.invokeFor(doGetObjectName(formattedName + ",extensionPoint="
                + extensionPoint.getName()),
                ExtensionPointInventoryMBean.class,
                new ExtensionPointInventoryMBeanAdapter(extensionPoint));

    }

    protected void doInventoryExtension(Callback callback,
            String formattedName, Extension extension) {
        formattedName += ",extensionPoint=" + extension.getExtensionPoint();
        Object[] contributions = extension.getContributions();
        if (contributions == null) return;
        for (Object contribution : contributions) {
            doInventoryContribution(callback, formattedName, contribution);
        }
    }

    private void doInventoryContribution(Callback callback,
            String formattedName, Object contribution) {
        formattedName += ",contribution="
                + Integer.toHexString(contribution.hashCode());
        callback.invokeFor(doGetObjectName(formattedName),
                contribution.getClass(), contribution);
    }
}
