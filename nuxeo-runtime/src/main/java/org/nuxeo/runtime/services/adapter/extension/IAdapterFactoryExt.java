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

import org.nuxeo.runtime.services.adapter.AdapterFactory;

/**
 * An internal interface that exposes portion of AdapterFactoryProxy functionality
 * without the need to import the class itself.
 */
public interface IAdapterFactoryExt {

    /**
     * Loads the real adapter factory, but only if its associated plug-in is
     * already loaded. Returns the real factory if it was successfully loaded.
     * @param force if <code>true</code> the plugin providing the
     *              factory will be loaded if necessary, otherwise no plugin activations
     *              will occur.
     */
    AdapterFactory loadFactory(boolean force);

    String[] getAdapterNames();
}
