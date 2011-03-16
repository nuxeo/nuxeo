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
package org.nuxeo.runtime.services.adapter;

import org.nuxeo.runtime.model.Adaptable;


/**
 * An abstract superclass implementing the <code>Adaptable</code>
 * interface. <code>getAdapter</code> invocations are directed
 * to the platform's adapter manager.
 * <p>
 * Note: In situations where it would be awkward to subclass this
 * class, the same affect can be achieved simply by implementing
 * the <code>IAdaptable</code> interface and explicitly forwarding
 * the <code>getAdapter</code> request to the platform's
 * adapter manager. The method would look like:
 * <pre>
 *     public Object getAdapter(Class adapter) {
 *         return NXRuntime.getAdapterManager().getAdapter(this, adapter);
 *     }
 * </pre>
 * <p>
 * This class can be used without OSGi running.
 * <p>
 * Clients may subclass.
 *
 * @see Adaptable
 */
public abstract class AdaptableObject implements Adaptable {

    /**
     * Returns an object which is an instance of the given class
     * associated with this object. Returns <code>null</code> if
     * no such object can be found.
     * <p>
     * This implementation of the method declared by <code>IAdaptable</code>
     * passes the request along to the platform's adapter manager; roughly
     * <code>Platform.getAdapterManager().getAdapter(this, adapter)</code>.
     * Subclasses may override this method (however, if they do so, they
     * should invoke the method on their superclass to ensure that the
     * Platform's adapter manager is consulted).
     *
     * @param adapter the class to adapt to
     * @return the adapted object or <code>null</code>
     * @see Adaptable#getAdapter(Class)
     */
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        //TODO
        return null;
    }
}
