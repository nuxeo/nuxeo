/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - added generics in method signatures
 */
package org.nuxeo.runtime.model;

/**
 * An interface for an adaptable object.
 * <p>
 * Adaptable objects can be dynamically extended to provide different interfaces (or "adapters"). Adapters are created
 * by adapter factories, which are in turn managed by type by adapter managers.
 * <p>
 * For example,
 *
 * <pre>
 *     IAdaptable a = [some adaptable];
 *     IFoo x = a.getAdapter(IFoo.class);
 *     if (x != null)
 *         [do IFoo things with x]
 * </pre>
 * <p>
 * This interface can be used without OSGi running.
 * <p>
 * Clients may implement this interface, or obtain a default implementation of this interface by subclassing
 * <code>PlatformObject</code>.
 */
public interface Adaptable {

    /**
     * Returns an object which is an instance of the given class associated with this object. Returns <code>null</code>
     * if no such object can be found.
     *
     * @param adapter the adapter class to look up
     * @return a object castable to the given class, or <code>null</code> if this object does not have an adapter for
     *         the given class
     */
    <T> T getAdapter(Class<T> adapter);

}
