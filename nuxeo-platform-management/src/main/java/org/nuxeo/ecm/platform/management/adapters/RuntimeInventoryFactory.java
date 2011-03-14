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

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.AbstractResourceFactory;
import org.nuxeo.runtime.management.ObjectNameFactory;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.ExtensionPoint;
import org.nuxeo.runtime.model.RegistrationInfo;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class RuntimeInventoryFactory extends AbstractResourceFactory {

    public void registerResources() {
        service.registerResource("runtime-inventory", ObjectNameFactory.formatQualifiedName("factory",
                "RuntimeInventory"), RuntimeInventoryMBean.class,
                new RuntimeInventoryAdapter(this));
    }

    public void bindTree() {
        doVisitInventory(new Callback() {
            public void invokeFor(String name, String qualifiedName, Class<?> info, Object instance) {
                service.registerResource(null, qualifiedName + ",management=inventory", info, instance);
            }
        });
    }

    public void unbindTree() {
        doVisitInventory(new Callback() {
            public void invokeFor(String name, String qualifiedName, Class<?> info, Object instance) {
                service.unregisterResource(null, qualifiedName + ",management=inventory");
            }
        });
    }

    private interface Callback {
        void invokeFor(String name, String qualifiedName, Class<?> info, Object instance);
    }

    protected void doVisitInventory(Callback callback) {
        for (RegistrationInfo info : Framework.getRuntime().getComponentManager().getRegistrations()) {
            doVisitInventoryComponent(callback, info);
        }
    }

    protected void doVisitInventoryComponent(Callback callback, RegistrationInfo info) {
        ComponentName componentName = info.getName();
        String name = componentName.getName();
        String qualifiedName = ObjectNameFactory.formatQualifiedName(componentName);
        callback.invokeFor(name, qualifiedName, ComponentInventoryMBean.class,
                new ComponentInventoryAdapter(info));
        for (ExtensionPoint extensionPoint : info.getExtensionPoints()) {
            doVisitInventoryExtensionPoint(callback, name, qualifiedName,
                    extensionPoint);
        }
        for (Extension extension : info.getExtensions()) {
            doVisitInventoryExtension(callback, name, qualifiedName, extension);
        }
    }

    protected void doVisitInventoryExtensionPoint(Callback callback,
            String name, String qualifiedName, ExtensionPoint extensionPoint) {
        name += "-" + extensionPoint.getName();
        qualifiedName += ",extensionPoint=" + extensionPoint.getName();
        callback.invokeFor(name, qualifiedName, ExtensionPointInventoryMBean.class,
                new ExtensionPointInventoryAdapter(extensionPoint));

    }

    protected void doVisitInventoryExtension(Callback callback,
            String name, String qualifiedName, Extension extension) {
        qualifiedName += ",extensionPoint=" + extension.getExtensionPoint();
        Object[] contributions = extension.getContributions();
        if (contributions == null) {
            return;
        }
        for (Object contribution : contributions) {
            doVisitInventoryContribution(callback, name, qualifiedName, contribution);
        }
    }

    private static void doVisitInventoryContribution(Callback callback,
            String name, String qualifiedName, Object contribution) {
        String hexName = Integer.toHexString(contribution.hashCode());
        name += "-" + hexName;
        qualifiedName += ",contribution=" + hexName;
        callback.invokeFor(name, qualifiedName, contribution.getClass(), contribution);
    }

}
