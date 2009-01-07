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

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.AbstractResourceFactory;
import org.nuxeo.runtime.management.ManagementServiceImpl;
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
public class RuntimeInventoryMBeanAdapterFactory extends
        AbstractResourceFactory implements ResourceFactory {

    public RuntimeInventoryMBeanAdapterFactory(ManagementServiceImpl service,
            ResourceFactoryDescriptor descriptor) {
        super(service, descriptor);
    }

    public void register() {
        service.registerResource("inventory", ObjectNameFactory.formatQualifiedName("factory",
                "RuntimeInventory"), RuntimeInventoryMBean.class,
                new RuntimeInventoryMBeanAdapter(this));
    }

    public void bindTree() {
        doVisitInventory(new Callback() {
            public void invokeFor(String name, Class<?> info, Object instance) {
                service.registerResource(null, name + ",info=inventory", info, instance);
            }
        });
    }
    
    public void unbindTree() {
        doVisitInventory(new Callback() {
            public void invokeFor(String name, Class<?> info, Object instance) {
                service.unregisterResource(ObjectNameFactory.formatShortName(name + ",info=inventory"));
            }
        });
    }

    private interface Callback {
        void invokeFor(String name, Class<?> info, Object instance);
    }

    protected void doVisitInventory(Callback callback) {
        for (RegistrationInfo info : Framework.getRuntime().getComponentManager().getRegistrations()) {
            doVisitInventoryComponent(new Callback() {
                public void invokeFor(String name, Class<?> info,
                        Object instance) {
                    service.registerResource(null, name + ",info=inventory", info,
                            instance);
                }
            }, info);
        }
    }

    protected void doVisitInventoryComponent(Callback callback,
            RegistrationInfo info) {
        String formattedName = ObjectNameFactory.formatQualifiedName(info.getName());
        callback.invokeFor(formattedName, ComponentInventoryMBean.class,
                new ComponentInventoryMBeanAdapter(info));
        for (ExtensionPoint extensionPoint : info.getExtensionPoints()) {
            doVisitInventoryExtensionPoint(callback, formattedName,
                    extensionPoint);
        }
        for (Extension extension : info.getExtensions()) {
            doVisitInventoryExtension(callback, formattedName, extension);
        }
    }

    protected void doVisitInventoryExtensionPoint(Callback callback,
            String formattedName, ExtensionPoint extensionPoint) {
        callback.invokeFor(formattedName + ",extensionPoint="
                + extensionPoint.getName(), ExtensionPointInventoryMBean.class,
                new ExtensionPointInventoryMBeanAdapter(extensionPoint));

    }

    protected void doVisitInventoryExtension(Callback callback,
            String formattedName, Extension extension) {
        formattedName += ",extensionPoint=" + extension.getExtensionPoint();
        Object[] contributions = extension.getContributions();
        if (contributions == null)
            return;
        for (Object contribution : contributions) {
            doVisitInventoryContribution(callback, formattedName, contribution);
        }
    }

    private void doVisitInventoryContribution(Callback callback,
            String formattedName, Object contribution) {
        formattedName += ",contribution="
                + Integer.toHexString(contribution.hashCode());
        callback.invokeFor(formattedName, contribution.getClass(), contribution);
    }
}
