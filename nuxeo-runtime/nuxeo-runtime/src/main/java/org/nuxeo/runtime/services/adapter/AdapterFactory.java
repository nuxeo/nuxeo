/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.nuxeo.runtime.services.adapter;

/**
 * An adapter factory defines behavioral extensions for
 * one or more classes that implements the <code>Adaptable</code>
 * interface. Adapter factories are registered with an
 * adapter manager.
 * <p>
 * This interface can be used without OSGi running.
 * <p>
 * Clients may implement this interface.
 *
 * @see AdapterManager
 * @see org.nuxeo.runtime.model.Adaptable
 */
public interface AdapterFactory {

    /**
     * Returns an object which is an instance of the given class
     * associated with the given object. Returns <code>null</code> if
     * no such object can be found.
     *
     * @param adaptableObject the adaptable object being queried
     *   (usually an instance of <code>IAdaptable</code>)
     * @param adapterType the type of adapter to look up
     * @return a object castable to the given adapter type,
     *    or <code>null</code> if this adapter factory
     *    does not have an adapter of the given type for the
     *    given object
     */
    Object getAdapter(Object adaptableObject, Class adapterType);

    /**
     * Returns the collection of adapter types handled by this
     * factory.
     * <p>
     * This method is generally used by an adapter manager
     * to discover which adapter types are supported, in advance
     * of dispatching any actual <code>getAdapter</code> requests.
     *
     * @return the collection of adapter types
     */
    Class[] getAdapterList();

}
