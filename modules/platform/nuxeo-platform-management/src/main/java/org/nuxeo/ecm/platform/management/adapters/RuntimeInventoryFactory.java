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

    @Override
    public void registerResources() {
        service.registerResource("runtime-inventory",
                ObjectNameFactory.formatQualifiedName("factory", "RuntimeInventory"), RuntimeInventoryMBean.class,
                new RuntimeInventoryAdapter(this));
    }

    public void bindTree() {
        doVisitInventory(new Callback() {
            @Override
            public void invokeFor(String name, String qualifiedName, Class<?> info, Object instance) {
                service.registerResource(null, qualifiedName + ",management=inventory", info, instance);
            }
        });
    }

    public void unbindTree() {
        doVisitInventory(new Callback() {
            @Override
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
        callback.invokeFor(name, qualifiedName, ComponentInventoryMBean.class, new ComponentInventoryAdapter(info));
        for (ExtensionPoint extensionPoint : info.getExtensionPoints()) {
            doVisitInventoryExtensionPoint(callback, name, qualifiedName, extensionPoint);
        }
        for (Extension extension : info.getExtensions()) {
            doVisitInventoryExtension(callback, name, qualifiedName, extension);
        }
    }

    protected void doVisitInventoryExtensionPoint(Callback callback, String name, String qualifiedName,
            ExtensionPoint extensionPoint) {
        name += "-" + extensionPoint.getName();
        qualifiedName += ",extensionPoint=" + extensionPoint.getName();
        callback.invokeFor(name, qualifiedName, ExtensionPointInventoryMBean.class, new ExtensionPointInventoryAdapter(
                extensionPoint));

    }

    protected void doVisitInventoryExtension(Callback callback, String name, String qualifiedName, Extension extension) {
        qualifiedName += ",extensionPoint=" + extension.getExtensionPoint();
        Object[] contributions = extension.getContributions();
        if (contributions == null) {
            return;
        }
        for (Object contribution : contributions) {
            doVisitInventoryContribution(callback, name, qualifiedName, contribution);
        }
    }

    private static void doVisitInventoryContribution(Callback callback, String name, String qualifiedName,
            Object contribution) {
        String hexName = Integer.toHexString(contribution.hashCode());
        name += "-" + hexName;
        qualifiedName += ",contribution=" + hexName;
        callback.invokeFor(name, qualifiedName, contribution.getClass(), contribution);
    }

}
