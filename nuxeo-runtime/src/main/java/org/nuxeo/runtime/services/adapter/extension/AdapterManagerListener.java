/* 
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.nuxeo.runtime.services.adapter.extension;


/**
 * Portions of the AdapterManager that deal with the Eclipse extension registry
 * were moved into this class.
 *
 * @since org.eclipse.core.runtime 3.2
 */
public final class AdapterManagerListener {
//    implements IRegistryChangeListener, IAdapterManagerProvider {
//
//    public static final String ADAPTER_POINT_ID = "org.eclipse.core.runtime.adapters"; //$NON-NLS-1$
//
//    private AdapterManager theAdapterManager;
//
//    /**
//     * Constructs a new adapter manager.
//     */
//    public AdapterManagerListener() {
//        theAdapterManager = AdapterManager.getDefault();
//        theAdapterManager.registerLazyFactoryProvider(this);
//    }
//
//    /**
//     * Loads adapters registered with the adapters extension point from
//     * the plug-in registry.  Note that the actual factory implementations
//     * are loaded lazily as they are needed.
//     */
//    public boolean addFactories(AdapterManager adapterManager) {
//        IExtensionPoint point = RegistryFactory.getRegistry().getExtensionPoint(ADAPTER_POINT_ID);
//        if (point == null)
//            return false;
//
//        boolean factoriesAdded = false;
//        IExtension[] extensions = point.getExtensions();
//        for (int i = 0; i < extensions.length; i++) {
//            IConfigurationElement[] elements = extensions[i].getConfigurationElements();
//            for (int j = 0; j < elements.length; j++) {
//                AdapterFactoryProxy proxy = AdapterFactoryProxy.createProxy(elements[j]);
//                if (proxy != null) {
//                    adapterManager.registerFactory(proxy, proxy.getAdaptableType());
//                    factoriesAdded = true;
//                }
//            }
//        }
//        RegistryFactory.getRegistry().addRegistryChangeListener(this);
//        return factoriesAdded;
//    }
//
//    private void registerExtension(IExtension extension) {
//        IConfigurationElement[] elements = extension.getConfigurationElements();
//        for (int j = 0; j < elements.length; j++) {
//            AdapterFactoryProxy proxy = AdapterFactoryProxy.createProxy(elements[j]);
//            if (proxy != null)
//                theAdapterManager.registerFactory(proxy, proxy.getAdaptableType());
//        }
//    }
//
//    public synchronized void registryChanged(IRegistryChangeEvent event) {
//        //find the set of changed adapter extensions
//        HashSet toRemove = null;
//        IExtensionDelta[] deltas = event.getExtensionDeltas();
//        boolean found = false;
//        for (int i = 0; i < deltas.length; i++) {
//            //we only care about extensions to the adapters extension point
//            if (!ADAPTER_POINT_ID.equals(deltas[i].getExtensionPoint().getUniqueIdentifier()))
//                continue;
//            found = true;
//            if (deltas[i].getKind() == IExtensionDelta.ADDED)
//                registerExtension(deltas[i].getExtension());
//            else {
//                //create the hash set lazily
//                if (toRemove == null)
//                    toRemove = new HashSet();
//                toRemove.add(deltas[i].getExtension().getUniqueIdentifier());
//            }
//        }
//        //need to discard cached state for the changed extensions
//        if (found)
//            theAdapterManager.flushLookup();
//        if (toRemove == null)
//            return;
//        //remove any factories belonging to extensions that are going away
//        for (Iterator it = theAdapterManager.getFactories().values().iterator(); it.hasNext();) {
//            for (Iterator it2 = ((List) it.next()).iterator(); it2.hasNext();) {
//                AdapterFactory factory = (AdapterFactory) it2.next();
//                if (factory instanceof AdapterFactoryProxy) {
//                    String ext = ((AdapterFactoryProxy) factory).getOwnerId();
//                    if (toRemove.contains(ext))
//                        it2.remove();
//                }
//            }
//        }
//    }
//
//    /*
//     * Shuts down the listener by removing the registry change listener. Should only be
//     * invoked during platform shutdown.
//     */
//    public synchronized void stop() {
//        RegistryFactory.getRegistry().removeRegistryChangeListener(this);
//    }
}
